package io.tony.photo.service.impl;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import io.tony.photo.exception.PhotoDuplicateException;
import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.IndexBatcher;
import io.tony.photo.service.PhotoChangedListener;
import io.tony.photo.service.PhotoChangedListener.EventType;
import io.tony.photo.service.PhotoChangedListener.PhotoChangedEvent;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.MetadataService;
import io.tony.photo.service.PhotoListener;
import io.tony.photo.service.PhotoStore;
import io.tony.photo.utils.FileOp;
import io.tony.photo.utils.Json;
import lombok.Getter;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PhotoStoreImpl implements PhotoStore {
  private static final Logger log = LoggerFactory.getLogger(PhotoStoreImpl.class);
  static final Map<String, String> acceptFileType;

  private static final int THUMBNAIL_WIDTH = 600;

  static {
    Map<String, String> temp = new HashMap<>();
    temp.put(".jpg", "jpg");
    temp.put(".png", "png");
    temp.put(".nef", "nef");
    temp.put(".cr2", "cr2");
    temp.put(".arw", "arw");
    temp.put(".pef", "pef");
    temp.put(".ptx", "ptx");
    temp.put(".raf", "raf");
    temp.put(".rw2", "rw2");
    acceptFileType = Collections.unmodifiableMap(temp);
  }

  private Path photoStore;
  private Path container;
  private Path metaData;
  private Path trashStore;
  private Path indexPath;
  private Path thumbnails;

  //meta-data lock
  private ConcurrentMap<String, Object> metaDataLock = new ConcurrentHashMap<>();

  @Getter
  private MetadataService metadataService;

  private List<PhotoChangedListener> beforeChangedListeners = new LinkedList<>();
  private List<PhotoChangedListener> afterChangedListeners = new LinkedList<>();

  private IndexBatcher<PhotoMetadata> indexBatcher;

  @Getter
  private PhotoIndexStore indexStore;

  /**
   * 是否正在刷新
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);


  public PhotoStoreImpl(String photoStoreFolder) {
    this.container = Paths.get(photoStoreFolder);
    this.photoStore = Paths.get(photoStoreFolder, "photos");
    this.metaData = Paths.get(photoStoreFolder, ".meta");
    this.trashStore = Paths.get(photoStoreFolder, ".trash");
    this.indexPath = Paths.get(photoStoreFolder, ".index");
    this.thumbnails = Paths.get(photoStoreFolder, ".thumbnails");
    Stream.of(container, photoStore, metaData, trashStore, indexPath, thumbnails).forEach(FileOp::createDirectoryQuietly);

    this.metadataService = new MetadataServiceImpl();
    this.indexStore = new LucenePhotoIndexStore(this.indexPath);
    this.indexBatcher = new IndexBatcher<>(p -> indexStore.index(p), new PhotoMetadata());

    this.beforeChangedListeners.add(event -> {
      if (event.getEventType() == EventType.META_CREATED) {
        PhotoMetadata photo = event.getPhoto();
        long start = System.currentTimeMillis();
        try {
          //create thumbnail for current photo
          int width = photo.getWidth();
          int thumbnailHeight = width == -1 ? 600 : (int) ((Double.valueOf(THUMBNAIL_WIDTH) / width) * photo.getHeight());
          Thumbnails.of(Paths.get(photo.getPath()).toFile())
            .outputQuality(1.0d).antialiasing(Antialiasing.ON).size(THUMBNAIL_WIDTH, thumbnailHeight)
            .allowOverwrite(true).outputFormat("jpg")
            .toFile(this.thumbnails.resolve(photo.getId() + ".jpg").toFile());

          log.info("Created thumbnail for image: {}, total times: {}ms", photo.getId(), (System.currentTimeMillis() - start));
        } catch (Exception e) {
          log.error("Failed to create thumbnails for file: {}", photo.getPath(), e);
        }
      }
    });
    this.afterChangedListeners.add(event -> {
      switch (event.getEventType()) {
        case ADD:
          indexStore.index(event.getPhoto());
          break;
      }
    });
  }

  @Override
  public boolean add(Path photo) {

    if (Files.notExists(photo)) {
      throw new IllegalStateException("File not exist");
    }

    try (InputStream fis = Files.newInputStream(photo)) {
      String photoId = DigestUtils.sha1Hex(fis);
      Path metaFile = metaData.resolve(photoId);
      if (Files.exists(metaFile)) {
        PhotoMetadata from = Json.from(metaFile, PhotoMetadata.class);
        if (from != null && Files.exists(Paths.get(from.getPath())) && photoId.equals(from.getId())) {
          throw new PhotoDuplicateException("The photo was duplicated.");
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Check file duplication failed.", e);
    }

    final PhotoMetadata photoMetadata = metadataService.readMetadata(photo);
    if (photoMetadata == null) {
      return false;
    }
    fireBeforeChangeListener(photoMetadata);
    final Date shootingDate = photoMetadata.getShootingDate();
    Calendar c = Calendar.getInstance();
    c.setTime(shootingDate);

    Path targetStoreFile =
      photoStore.resolve(c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/" + photo.getFileName().toString());
    if (Files.notExists(targetStoreFile.getParent())) {
      FileOp.createDirectoriesQuietly(targetStoreFile.getParent());
    }

    try {
      if (Files.notExists(targetStoreFile) || !Files.isSameFile(photo, targetStoreFile)) {
        Files.copy(photo, targetStoreFile);
      }
      photoMetadata.setPath(targetStoreFile.toFile().getCanonicalPath());
      writeMetaData(photoMetadata);
      firePostChangeListener(photoMetadata);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Failed move photo from {} to {}.", photo, targetStoreFile, e);
    }
    return false;
  }

  @Override
  public boolean add(InputStream photoStream) {
    return false;
  }

  private void firePostChangeListener(PhotoMetadata photoMetadata) {
    PhotoChangedEvent after = new PhotoChangedEvent(photoMetadata, EventType.ADD);
    this.afterChangedListeners.forEach(l -> l.onChanged(after));
  }

  private void fireBeforeChangeListener(PhotoMetadata photoMetadata) {
    PhotoChangedEvent before = new PhotoChangedEvent(photoMetadata, EventType.META_CREATED);
    this.beforeChangedListeners.forEach(l -> l.onChanged(before));
  }

  private void writeMetaData(PhotoMetadata metadata) {
    Object lockMe = new Object();
    Object prevLock;
    while ((prevLock = metaDataLock.put(metadata.getId(), lockMe)) != null && (prevLock != lockMe)) {
      synchronized (prevLock) {
        try {
          prevLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    try (OutputStream metaOs = Files.newOutputStream(metaData.resolve(metadata.getId()))) {
      metaOs.write(Json.toJson(metadata).getBytes(UTF_8));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      metaDataLock.remove(metadata.getId(), lockMe);
      synchronized (lockMe) {
        lockMe.notifyAll();
      }
    }
  }

  @Override
  public boolean add(List<Path> photos) {
    return photos.stream().map(this::add).filter(r -> !r).count() > 0;
  }

  @Override
  public boolean remove(String id) {
    return true;
  }

  @Override
  public boolean remove(Path path) {
    return true;
  }

  @Override
  public Set<String> addTag(String id, String... tags) {
    final Path resolve = metaData.resolve(id);
    final PhotoMetadata from = Json.from(resolve, PhotoMetadata.class);
    if (from == null) {
      return Collections.emptySet();
    }
    final Set<String> originTags = from.getTags();
    Set<String> newTags = new HashSet<>();
    newTags.addAll(originTags);
    Stream.of(tags).forEach(newTags::add);
    from.setTags(newTags);
    writeMetaData(from);
    return newTags;
  }

  @Override
  public void importPhoto(Path sourceDirectory) {
    try {
      Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          final String fileName = file.getFileName().toString();
          int lastDotIndex = -1;
          if (fileName != null && !fileName.isBlank() && (lastDotIndex = fileName.lastIndexOf('.')) > 0) {
            String fileExtension = fileName.substring(lastDotIndex);
            if (acceptFileType.containsKey(fileExtension.toLowerCase())) {
              add(file);
            }
          }
          return super.visitFile(file, attrs);
        }
      });
    } catch (IOException e) {
      log.error("Failed to visitor directory: {}", sourceDirectory);
    }
  }

  @Override
  public PhotoMetadata getMetadataFromDisk(String metadataId) {
    Path metadataPath = metaData.resolve(metadataId);
    if (Files.exists(metadataPath)) {
      try {
        return Json.from(metadataPath, PhotoMetadata.class);
      } catch (Exception e) {
        log.error("Reading data failed: {}", metadataId, e);
      }
    }
    return null;
  }

  @Override
  public void refresh() {
    if (refreshing.get()) {
      log.info("Refreshing is ongoing, do nothing...");
      return;
    }

    if (refreshing.compareAndSet(false, true)) {
      log.info("Starting refreshing process...");
      long start = System.currentTimeMillis();
      try {
        AtomicInteger counter = new AtomicInteger();
        Files.walkFileTree(this.photoStore, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            final String fileName = file.getFileName().toString();
            int lastDotIndex = -1;
            if (fileName != null && !fileName.isBlank() && (lastDotIndex = fileName.lastIndexOf('.')) > 0) {
              String fileExtension = fileName.substring(lastDotIndex);
              if (acceptFileType.containsKey(fileExtension.toLowerCase())) {
                log.info("Refresh metadata and index: {}", file.toFile().getCanonicalFile());
                PhotoMetadata metadata = metadataService.readMetadata(file);
                metadata.setPath(file.toFile().getCanonicalPath());

                Path previousMeta = metaData.resolve(metadata.getId());
                if (Files.exists(previousMeta)) {
                  PhotoMetadata origin = Json.from(previousMeta, PhotoMetadata.class);
                  if (origin.getTags() != null) {
                    metadata.setTags(origin.getTags());
                  }
                  if (origin.getAlbum() != null) {
                    metadata.setAlbum(origin.getAlbum());
                  }
                }
                counter.incrementAndGet();
                fireBeforeChangeListener(metadata);
                writeMetaData(metadata);
                firePostChangeListener(metadata);
              }
            }
            return super.visitFile(file, attrs);
          }
        });

        log.info("Finished refreshing metadata, total files: {}, index docs: {}, elapsed: {}ms",
          counter.get(), this.indexStore.total(), System.currentTimeMillis() - start);
      } catch (IOException e) {
        log.error("Refresh directory failed.", e);
      } finally {
        refreshing.compareAndSet(true, false);
      }
    }
  }

  @Override
  public Path getThumbnail(String photoId) {
    return this.thumbnails.resolve(photoId + ".jpg");
  }


  @Override
  public void close() throws IOException {
    this.indexBatcher.close();
    this.indexStore.close();
  }

  public static void main(String[] args) {
    Random r = new Random();
    PhotoStoreImpl ps = new PhotoStoreImpl(args[0]);
    String[] testTags = {"猪八戒", "团建", "人物"};
    String[] testAlbums = {"猪八戒", "铁山坪"};
    ps.beforeChangedListeners.add(event -> {
      Set<String> tags = new HashSet<>();
      if (event.getPhoto().getTags() != null) {
        tags.addAll(event.getPhoto().getTags());
      }
      tags.add(testTags[r.nextInt(testTags.length)]);
      event.getPhoto().setTags(tags);

      if (r.nextInt(10) % 2 == 0) {
        Set<String> albums = new HashSet<>();
        if (event.getPhoto().getAlbum() != null) {
          albums.addAll(event.getPhoto().getAlbum());
        }
        albums.add(testAlbums[r.nextInt(testAlbums.length)]);
        event.getPhoto().setAlbum(albums);
      }
    });
    ps.refresh();
  }
}
