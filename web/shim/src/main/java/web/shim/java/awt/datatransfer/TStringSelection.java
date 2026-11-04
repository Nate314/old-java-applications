package web.shim.java.awt.datatransfer;

/** Text for the clipboard. */
public class TStringSelection implements TTransferable, TClipboardOwner {
    private final String data;

    public TStringSelection(String data) {
        this.data = data;
    }

    String text() {
        return this.data;
    }

    @Override
    public void lostOwnership(TClipboard clipboard, TTransferable contents) {
    }
}
