package web.nathanos;

import web.rt.Classes;

/**
 * Entry point of the web NathaNOS. NathaNOS finds each app by the Main-Class named in a jar's manifest, but TeaVM
 * only compiles classes the program refers to, and its Class.forName cannot find most apps' Main (a class with only
 * a static main is never instantiated). Registering them here does both. Keep this list in step with the jars in
 * web/nathanos/pom.xml (the old assets/Clock.jar provides clock.Main).
 */
public final class Boot {
    private Boot() {
    }

    public static void main(String[] args) {
        Classes.register(
            clock.Main.class,
            circlewindow.Main.class,
            flappyBird.Main.class,
            minesweeper.Main.class,
            rubikscube.Main.class,
            screensaver.Main.class,
            animate2.Animate2Main.class,
            stockchecker.Main.class,
            synthesizer.Main.class,
            ultimateticktacktoe.Main.class,
            groupcreator.Main.class,
            musictheoryexpert.Main.class,
            numberguess.Main.class,
            simplealgorithms.Main.class,
            tankgame.Main.class);
        main.Main.main(args);
    }
}
