package web.shim.javax.swing;

/** Checks the splice a text field's document reports when the browser changes the field's text (typing, paste, cut). */
public class TextChangeCheck {
    public static void main(String[] args) {
        assert TextChange.between("abc", "abc") == null : "no change, no event";
        expect(TextChange.between("Ann", "Ann,"), 3, 0, 1, "a key typed at the end");
        expect(TextChange.between("Ann,", "Ann"), 3, 1, 0, "Backspace");
        expect(TextChange.between("", "Ann, Bo, Cy"), 0, 0, 11, "a paste into an empty field");
        expect(TextChange.between("Ann, Cy", "Ann, Bo, Cy"), 5, 0, 4, "a paste in the middle");
        expect(TextChange.between("Ann, Bo, Cy", "Ann, Cy"), 5, 4, 0, "a cut from the middle");
        expect(TextChange.between("aaa", "aaaa"), 3, 0, 1, "repeated letters count from the end");
        expect(TextChange.between("4", "7"), 0, 1, 1, "a selection typed over");
        System.out.println("TextChangeCheck passed");
    }

    private static void expect(TextChange c, int offset, int removed, int inserted, String what) {
        assert c != null && c.offset == offset && c.removed == removed && c.inserted == inserted
                : what + ": got " + (c == null ? "null" : c.offset + "," + c.removed + "," + c.inserted);
    }
}
