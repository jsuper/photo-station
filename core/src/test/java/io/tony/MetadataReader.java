package io.tony;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;

import io.tony.photo.pojo.Photo;
import io.tony.photo.service.MetadataService;
import io.tony.photo.service.impl.MetadataServiceImpl;
import io.tony.photo.utils.Json;

public class MetadataReader {

  public static void main(String[] args) throws ImageProcessingException, IOException, MetadataException {
//    String img = "C:\\Users\\Ling\\Pictures\\devops.png";
//    String img = "D:\\photos\\photos\\2015\\3\\IMG_4243.JPG";
//    String img = "D:\\test-photos\\photos\\2017\\8\\IMG_20170914_162704.jpg";
    String img = "C:\\Users\\Ling\\Desktop\\mytest.jpg";
    MetadataService ms = new MetadataServiceImpl();
    Photo photo = ms.readMetadata(Paths.get(img));
    System.out.println(Json.toJson(photo.getCamera()));

    Metadata metadata = ImageMetadataReader.readMetadata(new File(img));

    ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
    Float shutter = exif.getFloatObject(ExifSubIFDDirectory.TAG_SHUTTER_SPEED);

    DecimalFormat format = new DecimalFormat("0.#");
    format.setRoundingMode(RoundingMode.HALF_UP);
    String shutterSpeed;
    if (shutter <= 1) {
      float apexPower = (float) (1 / (Math.exp(shutter * Math.log(2))));
      long apexPower10 = Math.round((double) apexPower * 10.0);
      float fApexPower = (float) apexPower10 / 10.0f;
      shutterSpeed = format.format(fApexPower);
    } else {
      int apexPower = (int) ((Math.exp(shutter * Math.log(2))));
      shutterSpeed = format.format(apexPower);
    }
    System.out.println(shutterSpeed);

    Double fl = exif.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
    Double ap = exif.getDouble(ExifSubIFDDirectory.TAG_APERTURE);
    Float exposure = exif.getFloatObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
    System.out.println("shutter: " + shutter + ", focal length: " + fl + ", aperture: " + ap + ", exposure: " + exposure);
    System.out.println(Math.pow(Math.sqrt(2), ap));

    metadata.getDirectories().forEach(dir -> {

//      if(dir.getClass() == ExifSubIFDDirectory.class) {
      System.out.println(dir.getName() + ":" + dir.getClass());
      dir.getTags().forEach(tag -> {
        System.out.println("\t" + tag.getTagName() + ":" + tag.getDescription() + ":" + tag.getTagType() + ":" + tag.getTagTypeHex());
      });
//      }
    });

    /*try (InputStream fis = Files.newInputStream(Paths.get(img))) {
      BufferedImage read = ImageIO.read(fis);
      System.out.println(read == null);

      if (read != null) {
        System.out.println("width: " + read.getWidth());
        System.out.println("height: " + read.getHeight());
      }
    } catch (Exception e) {
    }*/

  }
}
