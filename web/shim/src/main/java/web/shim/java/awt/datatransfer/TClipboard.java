package web.shim.java.awt.datatransfer;

import web.rt.Js;

/**
 * The system clipboard is the browser's: setContents writes text with navigator.clipboard (or a hidden text area
 * and the copy command where that is refused, as in an iframe without clipboard-write). Browsers allow it only
 * soon after a click or key press, which is when apps copy. Reading it back is not supported.
 */
public class TClipboard {
    private final String name;

    public TClipboard(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setContents(TTransferable contents, TClipboardOwner owner) {
        if (contents instanceof TStringSelection) {
            Js.copyText(((TStringSelection) contents).text());
        }
    }
}
