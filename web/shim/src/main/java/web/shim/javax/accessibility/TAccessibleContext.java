package web.shim.javax.accessibility;

import web.shim.java.awt.TComponent;

/**
 * Swing's accessible name and description for a component. On the page they become the element's
 * {@code aria-label} and {@code aria-description}, which is what a screen reader reads, as the Java Access Bridge
 * reads them on the desktop.
 */
public class TAccessibleContext {
    private final TComponent owner;
    private String name;
    private String description;

    public TAccessibleContext(TComponent owner) {
        this.owner = owner;
    }

    public String getAccessibleName() {
        return name;
    }

    public void setAccessibleName(String name) {
        this.name = name;
        owner.syncAccessible();
    }

    public String getAccessibleDescription() {
        return description;
    }

    public void setAccessibleDescription(String description) {
        this.description = description;
        owner.syncAccessible();
    }
}
