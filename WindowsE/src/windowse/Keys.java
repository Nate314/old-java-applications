package windowse;

/** Where the presses go: the real keyboard ({@link RobotKeys}) or, in a check, a list. */
interface Keys {
   void press(int keyCode);

   void release(int keyCode);
}
