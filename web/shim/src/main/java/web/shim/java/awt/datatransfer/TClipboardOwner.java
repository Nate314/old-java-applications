package web.shim.java.awt.datatransfer;

public interface TClipboardOwner {
    void lostOwnership(TClipboard clipboard, TTransferable contents);
}
