package web.teavm;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.InputStream;
import org.teavm.asm.ClassReader;
import org.teavm.asm.tree.ClassNode;
import org.teavm.model.ClassHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.Parser;

/** Checks which listener classes ShimPlugin marks web.rt.Drags: only app classes whose mouseDragged does something. */
public class DragsCheck {
    static int drags;

    static class Body implements MouseMotionListener {
        public void mouseDragged(MouseEvent e) { drags++; }
        public void mouseMoved(MouseEvent e) { }
    }

    static class Empty implements MouseMotionListener {
        public void mouseDragged(MouseEvent e) { }
        public void mouseMoved(MouseEvent e) { drags++; }
    }

    static class Hover extends MouseMotionAdapter {
        @Override public void mouseMoved(MouseEvent e) { drags++; }
    }

    static class Adapter extends MouseAdapter {
        @Override public void mouseDragged(MouseEvent e) { if (e != null) drags++; }
    }

    static class OnlyReturns extends MouseAdapter {
        @Override public void mouseDragged(MouseEvent e) { return; }
    }

    static class TwoArgs {
        public void mouseDragged(int x, int y) { drags += x + y; }
    }

    abstract static class Abstract implements MouseMotionListener {
        public abstract void mouseDragged(MouseEvent e);
    }

    /** Parses a sample's class file as TeaVM does, under an app-like name unless keepName, then runs the plugin. */
    static boolean marked(Class<?> sample, boolean keepName) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = sample.getResourceAsStream(sample.getName().replaceFirst(".*\\.", "") + ".class")) {
            new ClassReader(in).accept(node, 0);
        }
        if (!keepName) {
            node.name = "app/" + sample.getSimpleName();
        }
        ClassHolder cls = new Parser(new ReferenceCache()).parseClass(node);
        new ShimPlugin().transformClass(cls, null);
        return cls.getInterfaces().contains("web.rt.Drags");
    }

    public static void main(String[] args) throws Exception {
        assert marked(Body.class, false) : "a mouseDragged with a body follows drags";
        assert marked(Adapter.class, false) : "a MouseAdapter overriding mouseDragged follows drags";
        assert !marked(Empty.class, false) : "an empty mouseDragged is hover-only";
        assert !marked(OnlyReturns.class, false) : "a mouseDragged that only returns is hover-only";
        assert !marked(Hover.class, false) : "a mouseMoved-only adapter is hover-only";
        assert !marked(TwoArgs.class, false) : "a two-argument mouseDragged is not a listener method";
        assert !marked(Abstract.class, false) : "an abstract mouseDragged has nothing to run";
        assert !marked(Body.class, true) : "the shim's own web.* classes are left alone";
        System.out.println("DragsCheck passed");
    }
}
