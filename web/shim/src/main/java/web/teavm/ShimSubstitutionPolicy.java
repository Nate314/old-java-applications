package web.teavm;

import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * Makes TeaVM compile {@code web.shim.java.awt.TColor} wherever bytecode references {@code java.awt.Color},
 * and likewise for the other packages below. This is TeaVM 0.16's replacement for the old
 * {@code mapPackageHierarchy} entries in {@code META-INF/teavm.properties}.
 * Rules are tried in service-loader order, so the shim jar must come before teavm-classlib on the classpath
 * (teavm-classlib ships its own tiny Color, Dimension and Point that would otherwise win).
 */
public class ShimSubstitutionPolicy extends SimpleSubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        sink.selectClasses(inPackage("java.awt", true)
                        .or(inPackage("javax.swing", true))
                        .or(inPackage("javax.sound.midi", true))
                        .or(inPackage("java.beans", true))
                        .or(inPackage("java.util.jar"))
                        .or(named("java.net.URLClassLoader")))
                .packagePrefix("web.shim.")
                .simpleNamePrefix("T");
    }
}
