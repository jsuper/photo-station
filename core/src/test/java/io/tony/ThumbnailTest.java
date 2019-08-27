package io.tony;

import net.coobird.thumbnailator.ThumbnailParameter;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;

import java.io.File;
import java.io.IOException;

public class ThumbnailTest {

  public static void main(String[] args) throws IOException {
    String dir = "D:\\MyDocuments\\Source\\photo-station\\frontend\\src\\assets\\examples\\";
    Thumbnails.of(new File(dir).listFiles())
      .antialiasing(Antialiasing.ON)
      .outputQuality(1.0)
      .size(600, 408).outputFormat("jpg").toFiles(new Rename() {
      @Override
      public String apply(String name, ThumbnailParameter param) {
        name = name.substring(0, name.indexOf('.'));
        return name + "_500";
      }
    });
  }
}
