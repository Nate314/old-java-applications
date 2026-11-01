package web.shim.javax.swing;

public class TJButton extends TAbstractButton {
    public TJButton() {
    }

    public TJButton(String text) {
        setText(text);
    }

    public TJButton(TIcon icon) {
        setIcon(icon);
    }

    public TJButton(String text, TIcon icon) {
        setText(text);
        setIcon(icon);
    }
}
