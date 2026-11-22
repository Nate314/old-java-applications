package web.shim.java.awt.geom;

/** java.awt.geom.NoninvertibleTransformException: AffineTransform.createInverse of a flat (zero determinant) matrix. */
public class TNoninvertibleTransformException extends Exception {
    public TNoninvertibleTransformException(String msg) {
        super(msg);
    }
}
