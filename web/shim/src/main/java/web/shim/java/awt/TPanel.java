package web.shim.java.awt;

public class TPanel extends TContainer {
    public TPanel() {
        this(new TFlowLayout());
    }

    public TPanel(TLayoutManager layout) {
        setLayout(layout);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}
