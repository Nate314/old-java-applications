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
      String blueName = "blue";
      String redName = "red";
      String yellowName = "yellow";
      String greenName = "green";
      String orangeName = "orange";
      String whiteName = "white";
      return colorName == blueName
         ? this.blue
         : (
            colorName == redName
               ? this.red
               : (colorName == redName ? this.red : (colorName == yellowName ? this.yellow : (colorName == greenName ? this.green : (colorName == orangeName ? this.orange : (colorName == whiteName ? this.white : null)))))
         );
   }
}
