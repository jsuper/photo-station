package io.tony.photo.service.impl;

import com.drew.imaging.FileType;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.file.FileTypeDirectory;
import com.drew.metadata.gif.GifImageDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.MetadataHandler;
import io.tony.photo.service.MetadataService;

public class MetadataServiceImpl implements MetadataService {
  private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

  private List<Consumer<PhotoMetadata>> metadataApplier = new LinkedList<>();
  private static final List<Class<? extends MetadataHandler>> handlers = Arrays.asList(
    PhotoTagHandler.class, LocationHandler.class);

  private static final Map<String, FileType> imageType;
  private static final Map<String, FileType> rawImageType;
  private static final Map<String, FileType> videoType;

  static {

    List<FileType> fileTypes = Arrays.asList(FileType.values());

    imageType = Collections.unmodifiableMap(fileTypes.stream()
      .filter(type -> type.getMimeType() != null && type.getMimeType().startsWith("image/"))
      .collect(Collectors.toMap(FileType::getName, Function.identity())));
    Map<String, FileType> images = new HashMap<>();

    images.put(FileType.Arw.getName(), FileType.Arw);
    images.put(FileType.Crw.getName(), FileType.Crw);
    images.put(FileType.Cr2.getName(), FileType.Cr2);
    images.put(FileType.Nef.getName(), FileType.Nef);
    images.put(FileType.Orf.getName(), FileType.Orf);
    images.put(FileType.Orf.getName(), FileType.Orf);
    images.put(FileType.Rw2.getName(), FileType.Rw2);
    rawImageType = Collections.unmodifiableMap(images);

    Map<String, FileType> collect = fileTypes.stream()
      .filter(type -> type.getMimeType() != null && type.getMimeType().startsWith("video/"))
      .collect(Collectors.toMap(FileType::getName, Function.identity()));
    videoType = Collections.unmodifiableMap(collect);

  }

  public MetadataServiceImpl() {
    handlers.forEach(handlers -> {
      MetadataHandler handler = createHandler(handlers);
      if (handler != null) {
        metadataApplier.add(handler::handle);
      }
    });
  }

  private MetadataHandler createHandler(Class<? extends MetadataHandler> handler) {
    if (handler == null) {
      return null;
    }
    try {
      Constructor<? extends MetadataHandler> declaredConstructor = handler.getDeclaredConstructor();
      MetadataHandler metadataHandler = declaredConstructor.newInstance();
      return metadataHandler;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot instantiate handler for " + handler, e);
    }
  }

  private FileType getImageType(String name) {
    if (imageType.containsKey(name)) {
      return imageType.get(name);
    }
    if (rawImageType.containsKey(name)) {
      return rawImageType.get(name);
    }
    return FileType.Unknown;
  }

  private int[] readImageWidthHeightFromMetadata(Metadata metadata) {
    try {
      int width = -1;
      int height = -1;
      for (Directory dir : metadata.getDirectories()) {
        for (Tag tag : dir.getTags()) {
          if (tag.getTagName().toLowerCase().matches("image\\s*height")) {
            height = dir.getInt(tag.getTagType());
          }
          if (tag.getTagName().toLowerCase().matches("image\\s*width")) {
            width = dir.getInt(tag.getTagType());
          }
          if (width > 0 && height > 0) {
            return new int[]{width, height};
          }
        }
      }
      return width != -1 && height != -1 ? new int[]{width, height} : null;
    } catch (MetadataException e) {
      log.error("Reading width,height from metadata failed.");
    }
    return null;
  }

  private PhotoMetadata createMetadataFromExif(Path file) {
    PhotoMetadata pm = new PhotoMetadata();
    pm.setTimestamp(System.currentTimeMillis());
    try (InputStream fis = Files.newInputStream(file)) {
      String photoId = DigestUtils.sha1Hex(fis);
      pm.setId(photoId);
      pm.setPath(file.toFile().getCanonicalPath());
      pm.setSize(Files.size(file));
      String suffix = file.getFileName().toString();
      suffix = suffix.substring(suffix.lastIndexOf('.') + 1);
      pm.setType(suffix.toLowerCase());

      Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
      if (metadata != null) {
        FileTypeDirectory fileType = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
        String fileTypeName = fileType.getString(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME);

        FileType imageType = getImageType(fileTypeName);

        int[] ints = readImageWidthHeightFromMetadata(metadata);
        if (ints != null && ints.length == 2) {
          pm.setWidth(ints[0]);
          pm.setHeight(ints[1]);
        }
        ExifSubIFDDirectory exifSub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifSub != null) {
          Date shootDate = exifSub.getDateDigitized();
          pm.setShootingDate(shootDate);
        }

        GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null) {
          GeoLocation geo = gps.getGeoLocation();
          if (geo != null) {
            pm.setLatitude(BigDecimal.valueOf(geo.getLatitude()).setScale(6, RoundingMode.HALF_UP).doubleValue());
            pm.setLongitude(BigDecimal.valueOf(geo.getLongitude()).setScale(6, RoundingMode.HALF_UP).doubleValue());
          } else {
            pm.setLatitude(-1d);
            pm.setLongitude(-1d);
          }
        }

        ExifIFD0Directory exifIfd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifIfd0 != null) {
          String make = exifIfd0.getString(ExifDirectoryBase.TAG_MAKE);
          String model = exifIfd0.getString(ExifDirectoryBase.TAG_MODEL);
          pm.setDevice(make + "/" + model);
        }

      }

      if (pm.getWidth() == 0 || pm.getHeight() == 0) {
        log.info("Reading image width and height from metadata fail, try to read it from image io");
        try {
          BufferedImage image = ImageIO.read(file.toFile());
          int width = image.getWidth();
          int height = image.getHeight();

          pm.setWidth(width);
          pm.setHeight(height);
        } catch (Exception e) {
          pm.setWidth(-1);
          pm.setHeight(-1);
        }
      }
    } catch (ImageProcessingException | IOException e) {
      log.error("Reading exif information from {} failed.", file, e);
    }

    try {
      if (pm.getShootingDate() == null) {
        BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
        FileTime fileTime = basicFileAttributes.creationTime();
        pm.setShootingDate(new Date(fileTime.toMillis()));
      }
    } catch (IOException e) {
      log.error("Reading file attribute failed.", e);
    }
    return pm;
  }

  @Override
  public PhotoMetadata readMetadata(Path photo) {
    final PhotoMetadata metadata = createMetadataFromExif(photo);
    if (metadata != null) {
      metadataApplier.forEach(c -> c.accept(metadata));
      return metadata;
    }
    return null;
  }
}
