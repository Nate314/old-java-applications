package tankgame;

/** What drives a tank each tick: a player's keys or the computer. */
public interface Controls
{
    boolean forward();

    boolean back();

    /** Turns counterclockwise. */
    boolean left();

    /** Turns clockwise. */
    boolean right();

    /** True once for each new press of fire; reading it clears it, so holding the key does not repeat. */
    boolean fire();
}
