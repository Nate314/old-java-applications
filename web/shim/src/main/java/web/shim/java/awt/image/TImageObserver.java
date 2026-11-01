package web.shim.java.awt.image;

import web.shim.java.awt.TImage;

public interface TImageObserver {
    int WIDTH = 1;
    int HEIGHT = 2;
    int ALLBITS = 32;

    boolean imageUpdate(TImage img, int infoflags, int x, int y, int width, int height);
}
