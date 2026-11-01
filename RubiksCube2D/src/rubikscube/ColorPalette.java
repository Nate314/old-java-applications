package rubikscube;

import java.awt.Color;

public final class ColorPalette {
   private Color blue = new Color(0, 0, 255);
   private Color red = new Color(255, 0, 0);
   private Color yellow = new Color(255, 255, 0);
   private Color green = new Color(0, 255, 0);
   private Color orange = new Color(255, 175, 0);
   private Color white = new Color(255, 255, 255);

   public final Color colorFor(String colorName) {
      switch (colorName) {
         case "blue": return this.blue;
         case "red": return this.red;
         case "yellow": return this.yellow;
         case "green": return this.green;
         case "orange": return this.orange;
         case "white": return this.white;
         default: return null;
      }
   }
}
