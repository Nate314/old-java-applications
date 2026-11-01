package web.shim.java.awt;

public class TFrame extends TWindow {
    private String title = "";
    private boolean undecorated;
    private boolean resizable = true;
    private TImage iconImage;

    public TFrame() {
    }

    public TFrame(String title) {
        setTitle(title);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
        syncDecorations();
    }

    @Override
    protected String windowTitle() {
        return title;
    }

    public boolean isUndecorated() {
        return undecorated;
    }

    public void setUndecorated(boolean b) {
        undecorated = b;
        syncDecorations();
        invalidate();
    }

    @Override
    protected boolean isDecoratedWindow() {
        return !undecorated;
    }

    public boolean isResizable() {
        return resizable;
    }

    public void setResizable(boolean b) {
        resizable = b;
    }

    public TImage getIconImage() {
        return iconImage;
    }

    public void setIconImage(TImage image) {
        iconImage = image;
        setTitleIcon(image);
    }
}
