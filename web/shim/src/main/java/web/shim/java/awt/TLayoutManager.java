package web.shim.java.awt;

public interface TLayoutManager {
    void addLayoutComponent(String name, TComponent comp);

    void removeLayoutComponent(TComponent comp);

    TDimension preferredLayoutSize(TContainer parent);

    TDimension minimumLayoutSize(TContainer parent);

    void layoutContainer(TContainer parent);
}
