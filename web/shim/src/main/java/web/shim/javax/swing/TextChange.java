package web.shim.javax.swing;

/**
 * How one text became another, as Swing's document reports it: {@code removed} characters at {@code offset}
 * were replaced by {@code inserted} new ones. Kept free of the DOM so the desktop can check it.
 */
final class TextChange {
    final int offset;
    final int removed;
    final int inserted;

    private TextChange(int offset, int removed, int inserted) {
        this.offset = offset;
        this.removed = removed;
        this.inserted = inserted;
    }

    /** The smallest single splice from {@code before} to {@code after}, or null when they are equal. */
    static TextChange between(String before, String after) {
        if (before.equals(after)) {
            return null;
        }
        int max = Math.min(before.length(), after.length());
        int start = 0;
        while (start < max && before.charAt(start) == after.charAt(start)) {
            start++;
        }
        int end = 0;
        while (end < max - start
                && before.charAt(before.length() - 1 - end) == after.charAt(after.length() - 1 - end)) {
            end++;
        }
        return new TextChange(start, before.length() - start - end, after.length() - start - end);
    }
}
