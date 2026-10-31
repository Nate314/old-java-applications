package web.shim.javax.swing;

/** The page skin stands in for every look and feel, so choosing one changes nothing. */
public class TUIManager {
    private TUIManager() {
    }

    public static String getSystemLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    public static String getCrossPlatformLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    public static void setLookAndFeel(String className) throws TUnsupportedLookAndFeelException {
    }
}
