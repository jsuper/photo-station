package io.tony.photo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import io.tony.photo.pojo.LocationInfo;
import io.tony.photo.pojo.PhotoMetadata;
import io.tony.photo.service.LbsService;
import io.tony.photo.service.MetadataHandler;

public class LocationHandler implements MetadataHandler {
  private static final Logger log = LoggerFactory.getLogger(LocationHandler.class);

  private static String defaultProviderClass = System.getProperty("lbs.provider", TxLbsService.class.getName());

  private LbsService lbsService;
  private Class<? extends LbsService> lbsServiceProvider;

  public LocationHandler() {

    try {
      this.lbsServiceProvider = (Class<? extends LbsService>) Class.forName(defaultProviderClass);
      Objects.requireNonNull(lbsServiceProvider, "Provider was missing.");

      Constructor<? extends LbsService> constructor = lbsServiceProvider.getDeclaredConstructor();
      Objects.requireNonNull(constructor);
      lbsService = constructor.newInstance();
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Can not found provider: " + defaultProviderClass);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Provider default constructor is missing: " + defaultProviderClass);
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new IllegalStateException("Can not create provider instance.", e);
    }
  }

  @Override
  public void handle(PhotoMetadata metadata) {
    if (lbsService != null) {
      if (metadata.getLatitude() > 0 && metadata.getLongitude() > 0) {

        if (log.isDebugEnabled()) {
          log.debug("Got location({},{}) from tecent api.", metadata.getLatitude(), metadata.getLongitude());
        }
        lbsService.getLocation(metadata.getLatitude(), metadata.getLongitude())
          .ifPresent(metadata::setLocationInfo);
      }
    }
  }
}
