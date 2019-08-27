package io.tony;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;

import java.io.File;

public class RawToJpg {

  public static void main(String[] args) throws Exception {
    String rawFile = "C:\\Users\\Ling\\OneDrive\\Pictures\\DSC_1006.NEF";
    /*
    int width = 6000;
    int height = 4000;

    String[] readerFileSuffixes = ImageIO.getReaderFileSuffixes();
    System.out.println(Arrays.toString(readerFileSuffixes));*/

    Metadata metadata = ImageMetadataReader.readMetadata(new File(rawFile));
    metadata.getDirectories().forEach(dir -> {
      String name = dir.getName();
      System.out.println("=========================" + name + "===============================");
      dir.getTags().forEach(tag -> {
        System.out.println(tag.getTagName() + ": " + tag.getDescription());
      });

    });

  }
}
