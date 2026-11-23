package web.shim.java.awt.geom;

/** java.awt.geom.PathIterator: every shim shape walks its outline as the JDK's does, and Graphics2D draws any Shape through it. */
public interface TPathIterator {
    int WIND_EVEN_ODD = 0;
    int WIND_NON_ZERO = 1;
    int SEG_MOVETO = 0;
    int SEG_LINETO = 1;
    int SEG_QUADTO = 2;
    int SEG_CUBICTO = 3;
    int SEG_CLOSE = 4;

    int getWindingRule();

    boolean isDone();

    void next();

    int currentSegment(float[] coords);

    int currentSegment(double[] coords);
}
