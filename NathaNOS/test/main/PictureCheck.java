package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Self-check for Paint's pictures: the text a picture is saved as reads back the same pixels and a damaged one is
 * refused, the drawing tools put paint where they should (a fill stays inside a closed outline), and the pictures
 * file keeps its pictures, names and limit. Run with -ea.
 */
public final class PictureCheck {
   private PictureCheck() {
   }

   public static void main(String[] args) throws IOException {
      Picture blank = new Picture("Blank");
      check(blank.isBlank(), "a new picture is blank");
      check(blank.encode().equals("Ijag"), "a blank picture is one white run: " + blank.encode());

      Picture art = new Picture("Art");
      PaintPanel.line(art, 0, 0, 199, 124, 0, 3);
      PaintPanel.box(art, 20, 20, 60, 50, 7);
      PaintPanel.oval(art, 100, 10, 160, 70, 6);
      PaintPanel.line(art, -10, 100, 300, 100, 2, 14); // runs off both edges without failing
      check(art.get(0, 0) == 3 && art.get(199, 124) == 3, "the line reaches both ends");
      check(art.get(20, 20) == 7 && art.get(60, 50) == 7 && art.get(40, 20) == 7 && art.get(40, 35) == Picture.WHITE, "the box is an outline");
      check(art.get(130, 10) == 6 && art.get(130, 70) == 6 && art.get(100, 40) == 6 && art.get(160, 40) == 6, "the ellipse touches its box");
      check(art.get(0, 100) == 14 && art.get(199, 102) == 14 && art.get(5, 97) == Picture.WHITE, "a brush line is five pixels thick");

      byte[] round = Picture.decode(art.encode());
      check(round != null && Arrays.equals(round, art.pixels), "a picture reads back from its text");
      check(Picture.decode("Ijaf") == null, "a picture one pixel short is refused");
      check(Picture.decode("Ijah") == null, "a picture one pixel long is refused");
      check(Picture.decode("Zjag") == null, "an unknown color is refused");
      check(Picture.decode("") == null, "an empty text is refused");

      Picture fillIn = new Picture("Fill");
      PaintPanel.oval(fillIn, 40, 20, 100, 90, 0);
      PaintPanel.fill(fillIn, 70, 55, 5);
      check(fillIn.get(70, 55) == 5 && fillIn.get(45, 55) == 5, "the fill fills the ellipse");
      check(fillIn.get(0, 0) == Picture.WHITE && fillIn.get(150, 55) == Picture.WHITE, "the fill stays inside the ellipse");
      int before = fillIn.version();
      fillIn.changed();
      check(fillIn.version() == before + 1, "a change moves the version on");
      PaintPanel.fill(fillIn, 0, 0, 0);
      check(fillIn.get(199, 124) == 0 && fillIn.get(70, 55) == 5, "the outside fills up to the outline");

      File file = File.createTempFile("nathanos-pictures", ".txt");
      file.deleteOnExit();
      Pictures pictures = Pictures.load(file);
      check(pictures.all().isEmpty() && pictures.nextName().equals("Picture 1"), "an empty file has no pictures");
      int[] changes = {0};
      pictures.onChange(() -> changes[0]++);
      Picture first = new Picture(pictures.nextName());
      first.pixels[0] = 3;
      check(pictures.put(first) && changes[0] == 1, "a picture is added");
      check(pictures.nextName().equals("Picture 2"), "the next name counts on");
      Picture second = new Picture(pictures.nextName(), art.pixels.clone());
      pictures.put(second);
      first.pixels[1] = 4;
      pictures.put(first);
      check(pictures.all().size() == 2 && changes[0] == 3, "putting a picture again saves it in place");

      Pictures again = Pictures.load(file);
      check(again.all().size() == 2, "both pictures load back");
      check(again.find("Picture 1").pixels[0] == 3 && again.find("Picture 1").pixels[1] == 4, "the first picture's latest pixels load back");
      check(Arrays.equals(again.find("Picture 2").pixels, art.pixels), "the second picture loads back");

      pictures.remove(first);
      check(pictures.nextName().equals("Picture 1") && Pictures.load(file).all().size() == 1, "a removed picture is gone from the file");

      try (FileOutputStream out = new FileOutputStream(file)) {
         out.write(("picture=Good:Ijag\npicture=Bad:Ijaf\nnonsense\npicture=Good:Ijag\n").getBytes(StandardCharsets.UTF_8));
      }
      Pictures damaged = Pictures.load(file);
      check(damaged.all().size() == 1 && damaged.find("Good") != null, "damaged and repeated lines are skipped");

      Pictures full = Pictures.load(new File(file.getPath() + ".missing"));
      for (int i = 0; i < Pictures.MAX; i++) {
         full.put(new Picture(full.nextName()));
      }
      new File(file.getPath() + ".missing").delete();
      check(full.isFull() && !full.put(new Picture(full.nextName())), "no more than " + Pictures.MAX + " pictures");

      System.out.println("PictureCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
