package windowse;

/** One step of a list of shortcuts: a shortcut and how many times to press it. */
final class Step {
   final Shortcut shortcut;
   final int times;

   Step(Shortcut shortcut, int times) {
      this.shortcut = shortcut;
      this.times = times;
   }

   /** How it is written on the list: "Win+E x3", "Win+Left". */
   String label() {
      return this.shortcut.combo() + (this.times > 1 ? " x" + this.times : "");
   }
}
