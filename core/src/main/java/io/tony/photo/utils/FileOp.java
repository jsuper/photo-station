package io.tony.photo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileOp {
  private static final Logger log = LoggerFactory.getLogger(FileOp.class);

  public static void createDirectoryQuietly(Path folder) {
    if (Files.notExists(folder) || !Files.isDirectory(folder)) {
      try {
        if (log.isDebugEnabled()) {
          log.debug("Create directory: {}", folder);
        }
        Files.createDirectory(folder);
      } catch (IOException e) {
        //ignore create error
        log.error("Create folder error: {}", folder);
      }
    }
  }

  public static void createDirectoriesQuietly(Path folder) {
    if (Files.notExists(folder) || !Files.isDirectory(folder)) {
      try {
        if (log.isDebugEnabled()) {
          log.debug("Create directory: {}", folder);
        }
        Files.createDirectories(folder);
      } catch (IOException e) {
        //ignore create error
        log.error("Create folder error: {}", folder);
      }
    }
  }
}
