package web.teavm;

import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

/**
 * NathaNOS loads each jar's Main-Class by name, then calls its {@code (Boolean)} constructor or its {@code main}
 * method through reflection. TeaVM only keeps reflection data that a policy asks for, so this exposes the
 * constructors and main method of every app class that declares a main method. (The class must also be compiled
 * into the page; the web NathaNOS does that in web.nathanos.Boot.)
 */
public class ShimReflectionPolicy extends SimpleReflectionPolicy {
    @Override
    protected void setup() {
        selectClasses(ShimReflectionPolicy::isAppMainClass).foundByName().reflectableMethods(namedEither("<init>", "main"));
    }

    private static boolean isAppMainClass(IntrospectClass<?> cls) {
        String name = cls.name();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("org.teavm.") || name.startsWith("web.")) {
            return false;
        }
        return cls.declaredMethods().stream().anyMatch(method -> method.name().equals("main"));
    }
}
