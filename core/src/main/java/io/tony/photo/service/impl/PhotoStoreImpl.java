package io.tony.photo.service.impl;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import io.tony.photo.exception.PhotoDuplicateException;
import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.IndexBatcher;
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

  //meta-data lock
  private ConcurrentMap<String, Object> metaDataLock = new ConcurrentHashMap<>();

  @Getter
  private MetadataService metadataService;

  private List<PhotoListener> listeners = new LinkedList<>();

  private IndexBatcher<PhotoMetadata> indexBatcher;

  @Getter
  private PhotoIndexStore indexStore;


  public PhotoStoreImpl(String photoStoreFolder) {
    this.container = Paths.get(photoStoreFolder);
    this.photoStore = Paths.get(photoStoreFolder, "photos");
    this.metaData = Paths.get(photoStoreFolder, ".meta");
    this.trashStore = Paths.get(photoStoreFolder, ".trash");
    this.indexPath = Paths.get(photoStoreFolder, ".index");
    Stream.of(container, photoStore, metaData, trashStore, indexPath).forEach(FileOp::createDirectoryQuietly);

    this.metadataService = new MetadataServiceImpl();
    this.indexStore = new LucenePhotoIndexStore(this.indexPath);
    this.indexBatcher = new IndexBatcher<>(p -> indexStore.index(p), new PhotoMetadata());
   /* this.listeners.add(new PhotoListener() {
      @Override
      public void before(PhotoMetadata metadata, OpType type) {
        //ignore
      }

      @Override
      public void after(PhotoMetadata metadata, OpType type) {
        indexBatcher.add(metadata);
      }
    });*/
    listeners.add(new PhotoListener() {
      @Override
      public void before(PhotoMetadata metadata, OpType type) {

      }

      @Override
      public void after(PhotoMetadata metadata, OpType type) {
        indexStore.index(metadata);
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
    this.listeners.forEach(listener -> listener.before(photoMetadata, PhotoListener.OpType.META_CREATE));
    final Date shootingDate = photoMetadata.getShootingDate();
    Calendar c = Calendar.getInstance();
    c.setTime(shootingDate);

    Path targetStoreFile =
      photoStore.resolve(c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/" + photo.getFileName().toString());
    if (Files.notExists(targetStoreFile.getParent())) {
      FileOp.createDirectoriesQuietly(targetStoreFile.getParent());
    }

    try {
      Files.copy(photo, targetStoreFile);
      photoMetadata.setPath(targetStoreFile.toFile().getCanonicalPath());
      writeMetaData(photoMetadata);

      this.listeners.forEach(after -> after.after(photoMetadata, PhotoListener.OpType.ADD));
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Failed move photo from {} to {}.", photo, targetStoreFile, e);
    }
    return false;
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
  public void close() throws IOException {
    this.indexBatcher.close();
    this.indexStore.close();
  }
}
