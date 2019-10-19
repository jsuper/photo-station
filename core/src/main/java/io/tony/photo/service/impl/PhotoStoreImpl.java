package io.tony.photo.service.impl;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.tony.photo.exception.PhotoDuplicateException;
import io.tony.photo.pojo.Photo;
import io.tony.photo.service.IndexBatcher;
import io.tony.photo.service.PhotoChangedListener;
import io.tony.photo.service.PhotoChangedListener.EventType;
import io.tony.photo.service.PhotoChangedListener.PhotoChangedEvent;
import io.tony.photo.service.PhotoIndexStore;
import io.tony.photo.service.MetadataService;
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

  private IndexBatcher<Photo> indexBatcher;

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
    this.indexBatcher = new IndexBatcher<>(p -> indexStore.index(p), new Photo());

    this.beforeChangedListeners.add(event -> {
      if (event.getEventType() == EventType.META_CREATED) {
        Photo photo = event.getPhoto();
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
          log.info("Starting to index document: {}", Json.toJson(event.getPhoto()));
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
        Photo from = Json.from(metaFile, Photo.class);
        if (from != null && Files.exists(Paths.get(from.getPath())) && photoId.equals(from.getId())) {
          throw new PhotoDuplicateException("The photo was duplicated.");
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Check file duplication failed.", e);
    }

    final Photo photoMetadata = metadataService.readMetadata(photo);
    if (photoMetadata == null) {
      return false;
    }
    fireBeforeChangeListener(photoMetadata);
    final Date shootingDate = photoMetadata.getDate();
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

  private void firePostChangeListener(Photo photoMetadata) {
    PhotoChangedEvent after = new PhotoChangedEvent(photoMetadata, EventType.ADD);
    this.afterChangedListeners.forEach(l -> l.onChanged(after));
  }

  private void fireBeforeChangeListener(Photo photoMetadata) {
    PhotoChangedEvent before = new PhotoChangedEvent(photoMetadata, EventType.META_CREATED);
    this.beforeChangedListeners.forEach(l -> l.onChanged(before));
  }

  private void writeMetaData(Photo metadata) {
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
    final Photo from = Json.from(resolve, Photo.class);
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
  public Photo getMetadataFromDisk(String metadataId) {
    Path metadataPath = metaData.resolve(metadataId);
    if (Files.exists(metadataPath)) {
      try {
        return Json.from(metadataPath, Photo.class);
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
                Photo metadata = metadataService.readMetadata(file);
                metadata.setPath(file.toFile().getCanonicalPath());

                Path previousMeta = metaData.resolve(metadata.getId());
                if (Files.exists(previousMeta)) {
                  Photo origin = Json.from(previousMeta, Photo.class);
                  if (origin.getTags() != null) {
                    metadata.setTags(origin.getTags());
                  }
                  if (origin.getAlbums() != null) {
                    metadata.setAlbums(origin.getAlbums());
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
  public void refreshIndexesFromMetadata() {
    try {
      long start = System.currentTimeMillis();
      List<Photo> photos = Files.list(this.metaData).map(meta -> {
        try {
          return Json.from(meta, Photo.class);
        } catch (Exception e) {
          return null;
        }

      }).filter(Objects::nonNull).collect(Collectors.toList());

      this.indexStore.index(photos);

      log.info("Finished refreshing index from metadata, total docs: {}, elapsed time: {}ms", photos.size(),
        (System.currentTimeMillis() - start));
    } catch (IOException e) {
      log.error("Failed index from metadata.");
    }
  }

  @Override
  public Path getThumbnail(String photoId) {
    return this.thumbnails.resolve(photoId + ".jpg");
  }

  @Override
  public Photo getPhotoById(String id) {
    Path metadataPath = this.metaData.resolve(id);
    if (Files.exists(metadataPath)) {
      return Json.from(metadataPath, Photo.class);
    }
    return null;
  }

  @Override
  public Photo update(Photo photo) {
    if (photo != null) {
      Photo metadataFromDisk = getMetadataFromDisk(photo.getId());
      metadataFromDisk.setTitle(photo.getTitle());
      metadataFromDisk.setNote(photo.getNote());

      metadataFromDisk.setAlbums(photo.getAlbums());
      metadataFromDisk.setTags(photo.getTags());
      metadataFromDisk.setFavorite(photo.getFavorite());

      writeMetaData(metadataFromDisk);
      indexStore.index(metadataFromDisk);
      return metadataFromDisk;
    }
    return null;
  }

  @Override
  public void flush(Photo photo) {
    writeMetaData(photo);
  }


  @Override
  public void close() throws IOException {
    this.indexBatcher.close();
    this.indexStore.close();
  }

  public static void main(String[] args) {
    Random r = new Random();
    PhotoStoreImpl ps = new PhotoStoreImpl(args[0]);
//    ps.refresh();
    ps.refreshIndexesFromMetadata();
  }
}
