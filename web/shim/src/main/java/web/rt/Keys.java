package web.rt;

/** Maps DOM keyCode values to AWT VK_ codes. Most are identical; these are the ones that differ. */
public final class Keys {
    private Keys() {
    }

    public static int toVk(int domCode) {
        switch (domCode) {
            case 13: return 10;    // Enter
            case 46: return 127;   // Delete
            case 45: return 155;   // Insert
            case 188: return 44;   // comma
            case 190: return 46;   // period
            case 191: return 47;   // slash
            case 186: case 59: return 59;   // semicolon (Chrome, Firefox)
            case 187: case 61: return 61;   // equals
            case 189: case 173: return 45;  // minus
            case 219: return 91;   // [
            case 220: return 92;   // backslash
            case 221: return 93;   // ]
            case 91: case 92: return 524;   // Windows / Meta
            case 93: return 525;   // context menu
            default: return domCode;
        }
    }

    /** The KEY_TYPED character for a keydown, or 0 if the key does not type one. */
    public static char typedChar(String key, int vk, int modifiers) {
        if ((modifiers & (2 | 4)) != 0) {
            return 0;
        }
        if (key.length() == 1) {
            return key.charAt(0);
        }
        switch (vk) {
            case 10: return '\n';
            case 8: return '\b';
            case 9: return '\t';
            case 27: return 27;
            case 127: return 127;
            default: return 0;
        }
    }

    public static char pressedChar(String key, int vk) {
        if (key.length() == 1) {
            return key.charAt(0);
        }
        switch (vk) {
            case 10: return '\n';
            case 8: return '\b';
            case 9: return '\t';
            case 27: return 27;
            case 127: return 127;
            default: return '\uFFFF';
        }
    }
}
