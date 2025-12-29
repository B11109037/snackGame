package game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class SpriteStore {

    public Image loadAndScale(String resourcePath, int w, int h) {
        String full = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        try (InputStream is = getClass().getResourceAsStream(full)) {
            if (is == null) {
                System.err.println("[SpriteStore] Resource not found: " + full);
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);

            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = out.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
