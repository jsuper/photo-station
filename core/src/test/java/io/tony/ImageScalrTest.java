package io.tony;

import org.imgscalr.Scalr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

public class ImageScalrTest {

  public static void main(String[] args) throws IOException {
    String dir = "D:\\MyDocuments\\Source\\photo-station\\frontend\\src\\assets\\examples";
    BufferedImage image = ImageIO.read(Paths.get(dir, "IMG_4074.JPG").toFile());
    BufferedImage scaledInstance = getScaledInstance(image, 500, 500, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, true);
    ImageIO.write(scaledInstance, "jpg", new File(dir + "/demo.jpg"));
  }

  public static BufferedImage getScaledInstance(BufferedImage img,
                                         int targetWidth,
                                         int targetHeight,
                                         Object hint,
                                         boolean higherQuality)
  {
    int type = (img.getTransparency() == Transparency.OPAQUE) ?
      BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
    BufferedImage ret = (BufferedImage)img;
    int w, h;
    if (higherQuality) {
      // Use multi-step technique: start with original size, then
      // scale down in multiple passes with drawImage()
      // until the target size is reached
      w = img.getWidth();
      h = img.getHeight();
    } else {
      // Use one-step technique: scale directly from original
      // size to target size with a single drawImage() call
      w = targetWidth;
      h = targetHeight;
    }

    do {
      if (higherQuality && w > targetWidth) {
        w /= 2;
        if (w < targetWidth) {
          w = targetWidth;
        }
      }

      if (higherQuality && h > targetHeight) {
        h /= 2;
        if (h < targetHeight) {
          h = targetHeight;
        }
      }

      BufferedImage tmp = new BufferedImage(w, h, type);
      Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    } while (w != targetWidth || h != targetHeight);

    return ret;
  }
}
