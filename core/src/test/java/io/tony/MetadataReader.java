package io.tony;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.jpeg.JpegDirectory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class MetadataReader {

  public static void main(String[] args) throws ImageProcessingException, IOException, MetadataException {
//    String img = "C:\\Users\\Ling\\Pictures\\devops.png";
    String img = "D:\\photos\\photos\\2015\\3\\IMG_4243.JPG";
//    String img = "D:\\test-photos\\photos\\2017\\8\\IMG_20170914_162704.jpg";
    Metadata metadata = ImageMetadataReader.readMetadata(new File(img));


    metadata.getDirectories().forEach(dir -> {

      System.out.println(dir.getName() + ":" + dir.getClass());
      dir.getTags().forEach(tag -> {
        System.out.println("\t" + tag.getTagName() + ":" + tag.getDescription() + ":" + tag.getTagType() + ":" + tag.getTagTypeHex());
      });
    });

    try (InputStream fis = Files.newInputStream(Paths.get(img))) {
      BufferedImage read = ImageIO.read(fis);
      System.out.println(read == null);

      if (read != null) {
        System.out.println("width: " + read.getWidth());
        System.out.println("height: " + read.getHeight());
      }
    } catch (Exception e) {
    }

  }
}
