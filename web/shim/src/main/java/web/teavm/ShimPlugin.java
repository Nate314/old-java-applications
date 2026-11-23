package web.teavm;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Compile-time rewrite of a few JDK calls inside app code, so app sources stay untouched:
 * {@code Thread.sleep(long)} goes to {@code web.rt.Sleep.sleep} (JDK semantics for negative values),
 * {@code System.exit(int)} goes to {@code web.rt.Exit.exit} (closes the app's windows on the page), and
 * URL/URLConnection stream opening goes to {@code web.rt.Net} (throws IOException on failed requests), and
 * {@code Class.getResource(String)} goes to {@code web.rt.Net.resource} (null: the page has no class path to look in).
 * A class whose {@code mouseDragged} has a body also gets the {@code web.rt.Drags} marker (hover-only listeners don't).
 */
public class ShimPlugin implements TeaVMPlugin, ClassHolderTransformer {
    private static final String DRAGS = "web.rt.Drags";
    private static final MethodReference SLEEP = new MethodReference("web.rt.Sleep", "sleep", ValueType.LONG, ValueType.VOID);
    private static final MethodReference EXIT = new MethodReference("web.rt.Exit", "exit", ValueType.INTEGER, ValueType.VOID);
    private static final MethodReference OPEN_STREAM = new MethodReference("web.rt.Net", "openStream",
            ValueType.object("java.net.URL"), ValueType.object("java.io.InputStream"));
    private static final MethodReference RESOURCE = new MethodReference("web.rt.Net", "resource",
            ValueType.object("java.lang.Class"), ValueType.object("java.lang.String"), ValueType.object("java.net.URL"));
    private static final MethodReference GET_INPUT_STREAM = new MethodReference("web.rt.Net", "getInputStream",
            ValueType.object("java.net.URLConnection"), ValueType.object("java.io.InputStream"));

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        String name = cls.getName();
        if (name.startsWith("org.teavm.") || name.startsWith("web.") || name.startsWith("java.") || name.startsWith("javax.")) {
            return;
        }
        if (followsDrags(cls)) {
            cls.getInterfaces().add(DRAGS);
        }
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() == null) {
                continue;
            }
            for (BasicBlock block : method.getProgram().getBasicBlocks()) {
                for (Instruction insn : block) {
                    if (insn instanceof InvokeInstruction) {
                        InvokeInstruction invoke = (InvokeInstruction) insn;
                        MethodReference target = invoke.getMethod();
                        if (invoke.getInstance() == null && isLang(target.getClassName(), "Thread")
                                && target.getName().equals("sleep") && target.parameterCount() == 1
                                && target.parameterType(0).equals(ValueType.LONG)) {
                            invoke.setMethod(SLEEP);
                        } else if (invoke.getInstance() == null && isLang(target.getClassName(), "System")
                                && target.getName().equals("exit")) {
                            invoke.setMethod(EXIT);
                        } else if (invoke.getInstance() != null && isLang(target.getClassName(), "Class")
                                && target.getName().equals("getResource") && target.parameterCount() == 1) {
                            Variable receiver = invoke.getInstance();
                            Variable resourceName = invoke.getArguments().get(0);
                            invoke.setType(InvocationType.SPECIAL);
                            invoke.setInstance(null);
                            invoke.setArguments(receiver, resourceName);
                            invoke.setMethod(RESOURCE);
                        } else if (invoke.getInstance() != null && target.parameterCount() == 0
                                && (isNet(target.getClassName(), "URL") && target.getName().equals("openStream")
                                    || (isNet(target.getClassName(), "URLConnection") || isNet(target.getClassName(), "HttpURLConnection"))
                                        && target.getName().equals("getInputStream"))) {
                            Variable receiver = invoke.getInstance();
                            invoke.setType(InvocationType.SPECIAL);
                            invoke.setInstance(null);
                            invoke.setArguments(receiver);
                            invoke.setMethod(target.getName().equals("openStream") ? OPEN_STREAM : GET_INPUT_STREAM);
                        }
                    }
                }
            }
        }
    }

    /** True when the class declares a one-argument mouseDragged that does more than return. */
    private static boolean followsDrags(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getName().equals("mouseDragged") && method.parameterCount() == 1 && method.getProgram() != null) {
                for (BasicBlock block : method.getProgram().getBasicBlocks()) {
                    for (Instruction insn : block) {
                        if (!(insn instanceof ExitInstruction || insn instanceof JumpInstruction || insn instanceof EmptyInstruction)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNet(String className, String simpleName) {
        return className.equals("java.net." + simpleName) || className.equals("org.teavm.classlib.java.net.T" + simpleName);
    }

    private static boolean isLang(String className, String simpleName) {
        return className.equals("java.lang." + simpleName) || className.equals("org.teavm.classlib.java.lang.T" + simpleName);
    }
}
