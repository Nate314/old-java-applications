package web.shim.java.awt.event;

import web.shim.java.awt.TComponent;

public class TKeyEvent extends TInputEvent {
    public static final int KEY_TYPED = 400;
    public static final int KEY_PRESSED = 401;
    public static final int KEY_RELEASED = 402;
    public static final int VK_UNDEFINED = 0;
    public static final char CHAR_UNDEFINED = '\uFFFF';

    private int keyCode;
    private char keyChar;

    public TKeyEvent(TComponent source, int id, long when, int modifiers, int keyCode, char keyChar) {
        super(source, id, when, modifiers);
        this.keyCode = keyCode;
        this.keyChar = keyChar;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public void setKeyChar(char keyChar) {
        this.keyChar = keyChar;
    }

    public boolean isActionKey() {
        return keyChar == CHAR_UNDEFINED;
    }

    @Override
    public String toString() {
        return "java.awt.event.KeyEvent[" + id + ",keyCode=" + keyCode + ",keyChar='" + keyChar + "']";
    }
}
