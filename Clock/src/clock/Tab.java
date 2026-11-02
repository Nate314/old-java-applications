package clock;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/** One tab of the window: draws itself in the area under the tab strip and takes the input aimed at it. */
interface Tab {
   /** The tab's name on the strip, which may carry its time while it runs ("Timer 4:59"). */
   String label(long now);

   /** The line of help at the bottom of the window. */
   String hint();

   void paint(Graphics2D g, Rectangle area, long now);

   /** A mouse press in the area; {@code rightButton} for button 3. */
   void press(int x, int y, boolean rightButton, long now);

   void drag(int x, int y, long now);

   void release(int x, int y, long now);

   /** A key release; true when the tab used it. */
   boolean key(int keyCode, long now);
}
