package rubikscube;

public final class Move {
   private Cube cube = null;
   private String[] strip = new String[]{"", "", ""};
   private String savedCorner = "";
   private String savedEdge = "";
   private String blue = "blue";
   private String red = "red";
   private String yellow = "yellow";
   private String green = "green";
   private String orange = "orange";
   private String white = "white";

   public Move(String faceColor, String key, int cubie, Cube cube) {
      this.cube = cube;
      if (faceColor == this.blue) {
         if (key == "w") {
            this.shiftColumnUpFromTopFace(cubie);
         }

         if (key == "a") {
            this.rotateRingAroundFrontCounterClockwise(cubie);
         }

         if (key == "s") {
            this.shiftColumnDownFromTopFace(cubie);
         }

         if (key == "d") {
            this.rotateRingAroundFrontClockwise(cubie);
            return;
         }
      } else if (faceColor == this.red) {
         if (key == "w") {
            this.shiftColumnUp(cubie);
         }

         if (key == "a") {
            this.shiftRowLeft(cubie, true);
         }

         if (key == "s") {
            this.shiftColumnDown(cubie);
         }

         if (key == "d") {
            this.shiftRowRight(cubie, true);
            return;
         }
      } else if (faceColor == this.yellow) {
         if (key == "w") {
            this.rotateRingAroundFrontCounterClockwise(cubie);
         }

         if (key == "a") {
            this.shiftSideColumnLeft(cubie, true);
         }

         if (key == "s") {
            this.rotateRingAroundFrontClockwise(cubie);
         }

         if (key == "d") {
            this.shiftSideColumnRight(cubie, true);
            return;
         }
      } else if (faceColor == this.green) {
         if (key == "w") {
            this.shiftColumnUp(cubie);
         }

         if (key == "a") {
            this.rotateRingAroundFrontClockwise(cubie);
         }

         if (key == "s") {
            this.shiftColumnDown(cubie);
         }

         if (key == "d") {
            this.rotateRingAroundFrontCounterClockwise(cubie);
            return;
         }
      } else if (faceColor == this.orange) {
         if (key == "w") {
            this.shiftColumnUp(cubie);
         }

         if (key == "a") {
            this.shiftRowLeft(cubie, false);
         }

         if (key == "s") {
            this.shiftColumnDown(cubie);
         }

         if (key == "d") {
            this.shiftRowRight(cubie, false);
            return;
         }
      } else if (faceColor == this.white) {
         if (key == "w") {
            this.rotateRingAroundFrontClockwise(cubie);
         }

         if (key == "a") {
            this.shiftSideColumnLeft(cubie, false);
         }

         if (key == "s") {
            this.rotateRingAroundFrontCounterClockwise(cubie);
         }

         if (key == "d") {
            this.shiftSideColumnRight(cubie, false);
         }
      }
   }

   private void shiftColumnUpFromTopFace(int cubie) {
      int firstPass;
      int secondPass;
      int column;
      int offset = (
               column = (secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 1) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 2) != 3 && secondPass != 6 && secondPass != 9
                  ? secondPass
                  : 3
            )
            == 1
         ? -2
         : (column == 2 ? 0 : (column == 3 ? 2 : column));

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.topFace.stickers[column + i * 3 - 1];
         this.cube.topFace.stickers[column + i * 3 - 1] = this.cube.frontFace.stickers[column + i * 3 - offset - 1];
         this.cube.frontFace.stickers[column + i * 3 - offset - 1] = this.cube.bottomFace.stickers[column + i * 3 - offset - 1];
         this.cube.bottomFace.stickers[column + i * 3 - offset - 1] = this.cube.backFace.stickers[column + i * 3 - offset - 1];
         this.cube.backFace.stickers[column + i * 3 - offset - 1] = this.strip[i];
         this.cube.refresh();
      }

      if (column == 1) {
         this.rotateFaceClockwise(this.cube.rightFace);
      } else {
         if (column == 3) {
            this.rotateFaceCounterClockwise(this.cube.leftFace);
         }
      }
   }

   private void shiftColumnUp(int cubie) {
      int firstPass;
      int secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 1) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 2;
      cubie = secondPass;
      cubie = secondPass != 3 && cubie != 6 && cubie != 9 ? cubie : 3;

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.frontFace.stickers[cubie + i * 3 - 1];
         this.cube.frontFace.stickers[cubie + i * 3 - 1] = this.cube.bottomFace.stickers[cubie + i * 3 - 1];
         this.cube.bottomFace.stickers[cubie + i * 3 - 1] = this.cube.backFace.stickers[cubie + i * 3 - 1];
         this.cube.backFace.stickers[cubie + i * 3 - 1] = this.cube.topFace.stickers[-1 * cubie + 4 + i * 3 - 1];
         this.cube.topFace.stickers[-1 * cubie + 4 + i * 3 - 1] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 1) {
         this.rotateFaceCounterClockwise(this.cube.leftFace);
      } else {
         if (cubie == 3) {
            this.rotateFaceClockwise(this.cube.rightFace);
         }
      }
   }

   private void shiftColumnDownFromTopFace(int cubie) {
      int firstPass;
      int secondPass;
      int column;
      int offset = (
               column = (secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 1) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 2) != 3 && secondPass != 6 && secondPass != 9
                  ? secondPass
                  : 3
            )
            == 1
         ? -2
         : (column == 2 ? 0 : (column == 3 ? 2 : column));

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.topFace.stickers[column + i * 3 - 1];
         this.cube.topFace.stickers[column + i * 3 - 1] = this.cube.backFace.stickers[column + i * 3 - offset - 1];
         this.cube.backFace.stickers[column + i * 3 - offset - 1] = this.cube.bottomFace.stickers[column + i * 3 - offset - 1];
         this.cube.bottomFace.stickers[column + i * 3 - offset - 1] = this.cube.frontFace.stickers[column + i * 3 - offset - 1];
         this.cube.frontFace.stickers[column + i * 3 - offset - 1] = this.strip[i];
         this.cube.refresh();
      }

      if (column == 1) {
         this.rotateFaceCounterClockwise(this.cube.rightFace);
      } else {
         if (column == 3) {
            this.rotateFaceClockwise(this.cube.leftFace);
         }
      }
   }

   private void shiftColumnDown(int cubie) {
      int firstPass;
      int secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 1) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 2;
      cubie = secondPass;
      cubie = secondPass != 3 && cubie != 6 && cubie != 9 ? cubie : 3;

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.frontFace.stickers[cubie + i * 3 - 1];
         this.cube.frontFace.stickers[cubie + i * 3 - 1] = this.cube.topFace.stickers[-1 * cubie + 4 + i * 3 - 1];
         this.cube.topFace.stickers[-1 * cubie + 4 + i * 3 - 1] = this.cube.backFace.stickers[cubie + i * 3 - 1];
         this.cube.backFace.stickers[cubie + i * 3 - 1] = this.cube.bottomFace.stickers[cubie + i * 3 - 1];
         this.cube.bottomFace.stickers[cubie + i * 3 - 1] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 1) {
         this.rotateFaceClockwise(this.cube.leftFace);
      } else {
         if (cubie == 3) {
            this.rotateFaceCounterClockwise(this.cube.rightFace);
         }
      }
   }

   private void shiftRowLeft(int cubie, boolean fromFrontFace) {
      if (fromFrontFace) {
         int firstPass;
         int secondPass = (firstPass = cubie != 1 && cubie != 2 && cubie != 3 ? cubie : 0) != 4 && firstPass != 5 && firstPass != 6 ? firstPass : 1;
         cubie = secondPass;
         cubie = secondPass != 7 && cubie != 8 && cubie != 9 ? cubie : 2;

         for (int i = 0; i < 3; i++) {
            this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
            this.cube.leftFace.stickers[cubie + i * 3] = this.cube.frontFace.stickers[cubie * 3 + i];
            this.cube.frontFace.stickers[cubie * 3 + i] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
            this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
            this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.strip[i];
            this.cube.refresh();
         }

         if (cubie == 0) {
            this.rotateFaceClockwise(this.cube.topFace);
            return;
         }

         if (cubie == 2) {
            this.rotateFaceCounterClockwise(this.cube.bottomFace);
            return;
         }
      } else {
         int firstPass;
         int secondPass = (firstPass = cubie != 1 && cubie != 2 && cubie != 3 ? cubie : 2) != 4 && firstPass != 5 && firstPass != 6 ? firstPass : 1;
         cubie = secondPass;
         cubie = secondPass != 7 && cubie != 8 && cubie != 9 ? cubie : 0;

         for (int i = 0; i < 3; i++) {
            this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
            this.cube.leftFace.stickers[cubie + i * 3] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
            this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
            this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.frontFace.stickers[cubie * 3 + i];
            this.cube.frontFace.stickers[cubie * 3 + i] = this.strip[i];
            this.cube.refresh();
         }

         if (cubie == 0) {
            this.rotateFaceCounterClockwise(this.cube.topFace);
            return;
         }

         if (cubie == 2) {
            this.rotateFaceClockwise(this.cube.bottomFace);
         }
      }
   }

   private void shiftRowRight(int cubie, boolean fromFrontFace) {
      if (fromFrontFace) {
         int firstPass;
         int secondPass = (firstPass = cubie != 1 && cubie != 2 && cubie != 3 ? cubie : 0) != 4 && firstPass != 5 && firstPass != 6 ? firstPass : 1;
         cubie = secondPass;
         cubie = secondPass != 7 && cubie != 8 && cubie != 9 ? cubie : 2;

         for (int i = 0; i < 3; i++) {
            this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
            this.cube.leftFace.stickers[cubie + i * 3] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
            this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
            this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.frontFace.stickers[cubie * 3 + i];
            this.cube.frontFace.stickers[cubie * 3 + i] = this.strip[i];
            this.cube.refresh();
         }

         if (cubie == 0) {
            this.rotateFaceCounterClockwise(this.cube.topFace);
            return;
         }

         if (cubie == 2) {
            this.rotateFaceClockwise(this.cube.bottomFace);
            return;
         }
      } else {
         int firstPass;
         int secondPass = (firstPass = cubie != 1 && cubie != 2 && cubie != 3 ? cubie : 2) != 4 && firstPass != 5 && firstPass != 6 ? firstPass : 1;
         cubie = secondPass;
         cubie = secondPass != 7 && cubie != 8 && cubie != 9 ? cubie : 0;

         for (int i = 0; i < 3; i++) {
            this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
            this.cube.leftFace.stickers[cubie + i * 3] = this.cube.frontFace.stickers[cubie * 3 + i];
            this.cube.frontFace.stickers[cubie * 3 + i] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
            this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
            this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.strip[i];
            this.cube.refresh();
         }

         if (cubie == 0) {
            this.rotateFaceClockwise(this.cube.topFace);
            return;
         }

         if (cubie == 2) {
            this.rotateFaceCounterClockwise(this.cube.bottomFace);
         }
      }
   }

   private void rotateRingAroundFrontClockwise(int cubie) {
      int firstPass;
      int secondPass = (firstPass = cubie < 4 && cubie > 0 ? 0 : cubie) < 7 && firstPass > 3 ? 3 : firstPass;
      cubie = secondPass;
      cubie = secondPass < 10 && cubie > 6 ? 6 : cubie;

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.topFace.stickers[cubie + i];
         this.cube.topFace.stickers[cubie + i] = this.cube.leftFace.stickers[cubie + i];
         this.cube.leftFace.stickers[cubie + i] = this.cube.bottomFace.stickers[cubie + i];
         this.cube.bottomFace.stickers[cubie + i] = this.cube.rightFace.stickers[cubie + i];
         this.cube.rightFace.stickers[cubie + i] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 0) {
         this.rotateFaceClockwise(this.cube.frontFace);
      } else {
         if (cubie == 6) {
            this.rotateFaceCounterClockwise(this.cube.backFace);
         }
      }
   }

   private void rotateRingAroundFrontCounterClockwise(int cubie) {
      int firstPass;
      int secondPass = (firstPass = cubie < 4 && cubie > 0 ? 0 : cubie) < 7 && firstPass > 3 ? 3 : firstPass;
      cubie = secondPass;
      cubie = secondPass < 10 && cubie > 6 ? 6 : cubie;

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.leftFace.stickers[cubie + i];
         this.cube.leftFace.stickers[cubie + i] = this.cube.topFace.stickers[cubie + i];
         this.cube.topFace.stickers[cubie + i] = this.cube.rightFace.stickers[cubie + i];
         this.cube.rightFace.stickers[cubie + i] = this.cube.bottomFace.stickers[cubie + i];
         this.cube.bottomFace.stickers[cubie + i] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 0) {
         this.rotateFaceCounterClockwise(this.cube.frontFace);
      } else {
         if (cubie == 6) {
            this.rotateFaceClockwise(this.cube.backFace);
         }
      }
   }

   private void shiftSideColumnLeft(int cubie, boolean fromRightFace) {
      int firstPass;
      int secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 0) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 1;
      cubie = secondPass;
      cubie = secondPass != 3 && cubie != 6 && cubie != 9 ? cubie : 2;
      if (fromRightFace) {
         cubie = cubie == 0 ? 2 : (cubie == 2 ? 0 : cubie);
      }

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
         this.cube.leftFace.stickers[cubie + i * 3] = this.cube.frontFace.stickers[cubie * 3 + i];
         this.cube.frontFace.stickers[cubie * 3 + i] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
         this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
         this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 0) {
         this.rotateFaceClockwise(this.cube.topFace);
      } else {
         if (cubie == 2) {
            this.rotateFaceCounterClockwise(this.cube.bottomFace);
         }
      }
   }

   private void shiftSideColumnRight(int cubie, boolean fromRightFace) {
      int firstPass;
      int secondPass = (firstPass = cubie != 1 && cubie != 4 && cubie != 7 ? cubie : 0) != 2 && firstPass != 5 && firstPass != 8 ? firstPass : 1;
      cubie = secondPass;
      cubie = secondPass != 3 && cubie != 6 && cubie != 9 ? cubie : 2;
      if (fromRightFace) {
         cubie = cubie == 0 ? 2 : (cubie == 2 ? 0 : cubie);
      }

      for (int i = 0; i < 3; i++) {
         this.strip[i] = this.cube.leftFace.stickers[cubie + i * 3];
         this.cube.leftFace.stickers[cubie + i * 3] = this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1];
         this.cube.backFace.stickers[10 - (cubie + 1) * 3 + i - 1] = this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1];
         this.cube.rightFace.stickers[-1 * cubie + (i + 1) * 3 - 1] = this.cube.frontFace.stickers[cubie * 3 + i];
         this.cube.frontFace.stickers[cubie * 3 + i] = this.strip[i];
         this.cube.refresh();
      }

      if (cubie == 0) {
         this.rotateFaceClockwise(this.cube.topFace);
      } else {
         if (cubie == 2) {
            this.rotateFaceCounterClockwise(this.cube.bottomFace);
         }
      }
   }

   private void rotateFaceClockwise(Face face) {
      this.savedCorner = face.stickers[0];
      this.savedEdge = face.stickers[1];
      face.stickers[0] = face.stickers[6];
      face.stickers[6] = face.stickers[8];
      face.stickers[8] = face.stickers[2];
      face.stickers[1] = face.stickers[3];
      face.stickers[3] = face.stickers[7];
      face.stickers[7] = face.stickers[5];
      face.stickers[2] = this.savedCorner;
      face.stickers[5] = this.savedEdge;
      this.cube.refresh();
   }

   private void rotateFaceCounterClockwise(Face face) {
      this.savedCorner = face.stickers[0];
      this.savedEdge = face.stickers[1];
      face.stickers[0] = face.stickers[2];
      face.stickers[2] = face.stickers[8];
      face.stickers[8] = face.stickers[6];
      face.stickers[1] = face.stickers[5];
      face.stickers[5] = face.stickers[7];
      face.stickers[7] = face.stickers[3];
      face.stickers[6] = this.savedCorner;
      face.stickers[3] = this.savedEdge;
      this.cube.refresh();
   }
}
