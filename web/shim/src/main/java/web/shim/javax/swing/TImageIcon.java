package web.shim.javax.swing;

import web.shim.java.awt.TComponent;
import web.shim.java.awt.TGraphics;
import web.shim.java.awt.TImage;

public class TImageIcon implements TIcon {
    private TImage image;
    private String description;

    public TImageIcon(String filename) {
        image = TImage.load(filename);
        description = filename;
    }

    public TImageIcon(TImage image) {
        this.image = image;
    }

    public TImage getImage() {
        return image;
    }

    public void setImage(TImage image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int getIconWidth() {
        return image == null ? -1 : image.getWidth(null);
    }

    @Override
    public int getIconHeight() {
        return image == null ? -1 : image.getHeight(null);
    }

    @Override
    public void paintIcon(TComponent c, TGraphics g, int x, int y) {
        g.drawImage(image, x, y, c);
    }
}
