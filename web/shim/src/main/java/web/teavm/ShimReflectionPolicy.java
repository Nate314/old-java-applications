package web.teavm;

import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

/**
 * NathaNOS loads each jar's Main-Class by name and calls its {@code (Boolean)} constructor through reflection.
 * TeaVM only keeps reflection data that a policy asks for, so this exposes the main class of the jar that
 * ships in {@code NathaNOS/assets} (the old Clock build, {@code clock.Main}).
 */
public class ShimReflectionPolicy extends SimpleReflectionPolicy {
    @Override
    protected void setup() {
        selectClass("clock.Main").foundByName().reflectableMethods(named("<init>"));
    }
}
