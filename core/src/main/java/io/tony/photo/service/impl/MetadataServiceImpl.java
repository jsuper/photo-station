package io.tony.photo.service.impl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.MetadataHandler;
import io.tony.photo.service.MetadataService;

public class MetadataServiceImpl implements MetadataService {
  private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

  private List<Consumer<PhotoMetadata>> metadataApplier = new LinkedList<>();
  private static final List<Class<? extends MetadataHandler>> handlers = Arrays.asList(
    PhotoTagHandler.class, LocationHandler.class
  );

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
        metadata.getDirectoriesOfType(ExifSubIFDDirectory.class).stream().findFirst().ifPresent(ed -> {
          Date dateDigitized = ed.getDateDigitized();
          pm.setShootingDate(dateDigitized);
        });

        metadata.getDirectoriesOfType(GpsDirectory.class)
          .stream().findFirst().ifPresent(gd -> {
          GeoLocation geoLocation = gd.getGeoLocation();
          if (geoLocation != null) {
            pm.setLatitude(BigDecimal.valueOf(geoLocation.getLatitude()).setScale(6, RoundingMode.HALF_UP).doubleValue());
            pm.setLongitude(BigDecimal.valueOf(geoLocation.getLongitude()).setScale(6, RoundingMode.HALF_UP).doubleValue());
          } else {
            pm.setLatitude(-1d);
            pm.setLongitude(-1d);
          }
        });

        metadata.getDirectoriesOfType(ExifIFD0Directory.class).stream()
          .findFirst().ifPresent(efd -> {
          String make = efd.getString(ExifDirectoryBase.TAG_MAKE);
          String model = efd.getString(ExifDirectoryBase.TAG_MODEL);
          pm.setDevice(make + "/" + model);
        });
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
