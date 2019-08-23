package io.tony;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import jdk.swing.interop.SwingInterOpUtils;

public class Demo {
  public static void main(String[] args) throws IOException {
    String dir = "D:\\MyDocuments\\Source\\photo-station\\frontend\\src\\assets\\examples";
    BufferedImage image = ImageIO.read(Paths.get(dir, "IMG_4074.JPG").toFile());

//
    int type = image.getType();
    System.out.println(type);
    ResampleOp resampleOp = new ResampleOp(500, 500);
    resampleOp.setFilter(ResampleFilters.getLanczos3Filter());
    BufferedImage bi = new BufferedImage(500, 500, 5);
    BufferedImage resize = resampleOp.doFilter(image, bi, 500, 500);

    ImageIO.write(resize, "jpeg", new File(dir + "/demo.jpeg"));
  }
}
