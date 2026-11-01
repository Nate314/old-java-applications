package web.rt;

import java.util.HashMap;
import java.util.Map;

/**
 * Classes a page registers to be loadable by name. TeaVM's Class.forName only finds classes that the program
 * creates instances of, so a launcher class with just a static main (most apps' Main) is registered here instead
 * (see web.nathanos.Boot).
 */
public final class Classes {
    private static final Map<String, Class<?>> byName = new HashMap<>();

    private Classes() {
    }

    public static void register(Class<?>... classes) {
        for (Class<?> cls : classes) {
            byName.put(cls.getName(), cls);
        }
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        Class<?> cls = byName.get(name);
        return cls != null ? cls : Class.forName(name);
    }
}
