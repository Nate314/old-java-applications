package rubikscube;

import javax.swing.JButton;

public final class Cube {
   protected CubeFrame frame = null;
   protected ColorPalette palette = new ColorPalette();
   private String blue = "blue";
   private String red = "red";
   private String yellow = "yellow";
   private String green = "green";
   private String orange = "orange";
   private String white = "white";
   protected Face topFace = null;
   protected Face frontFace = null;
   protected Face rightFace = null;
   protected Face bottomFace = null;
   protected Face backFace = null;
   protected Face leftFace = null;
   protected String selectedFaceColor = "";

   public Cube(CubeFrame frame, JButton[][] faceButtons, int cellWidth, int cellHeight) {
      this.frame = frame;
      int[] identityLayout = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
      int[] rotated180Layout = new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1};
      int[] rotatedCounterClockwiseLayout = new int[]{7, 4, 1, 8, 5, 2, 9, 6, 3};
      int[] rotatedClockwiseLayout = new int[]{3, 6, 9, 2, 5, 8, 1, 4, 7};
      this.topFace = new Face(this, this.blue, faceButtons[0], rotated180Layout, cellWidth, cellHeight);
      this.frontFace = new Face(this, this.red, faceButtons[1], identityLayout, cellWidth, cellHeight);
      this.rightFace = new Face(this, this.yellow, faceButtons[2], rotatedCounterClockwiseLayout, cellWidth, cellHeight);
      this.bottomFace = new Face(this, this.green, faceButtons[3], identityLayout, cellWidth, cellHeight);
      this.backFace = new Face(this, this.orange, faceButtons[4], identityLayout, cellWidth, cellHeight);
      this.leftFace = new Face(this, this.white, faceButtons[5], rotatedClockwiseLayout, cellWidth, cellHeight);
   }

   public final void refresh() {
      this.topFace.refresh();
      this.frontFace.refresh();
      this.rightFace.refresh();
      this.bottomFace.refresh();
      this.backFace.refresh();
      this.leftFace.refresh();
   }
}
