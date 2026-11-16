package circlewindow;

/** Checks Send's codes: every shape, a word with its odd characters and a flipbook come back as they were sent. */
public final class LinkCheck {
   public static void main(String[] args) {
      for (String shape : Shapes.ALL) {
         for (String colors : CircleCreatorWindow.COLOR_NAMES) {
            Link back = Link.parse(Link.code(shape, colors, "HI", ""));
            assert back.shape.equals(shape) && back.colors.equals(colors) : shape + " " + colors + " comes back";
         }
      }

      String word = Link.code(Shapes.WORD, "Sunset", "it's 9:05?", "");
      assert word.equals("WSIT'S_9*05~") : "a word keeps to characters a link leaves alone: " + word;
      assert Link.parse(word).word.equals("IT'S 9:05?") : "the word comes back";
      assert Link.parse("https://x.example/CircleSpawner/?shape=" + word + "&v=2").word.equals("IT'S 9:05?")
         : "read from a whole link, up to the next parameter";

      DrawPad pad = new DrawPad(() -> { });
      pad.smile();
      pad.addFrame();
      String code = Link.code(Shapes.DRAW, "Rainbow", "", pad.text());
      assert code.length() == 2 + 3 * 67 + 2 : "three frames of 67 characters: " + code.length();
      assert code.matches("DR[A-Za-z0-9_.-]+") : "only characters a link leaves alone: " + code;
      Link drawing = Link.parse(code);
      assert drawing.drawing.equals(pad.text()) : "every frame comes back cell for cell";

      assert Link.parse("") == null && Link.parse("XR") == null && Link.parse("CX") == null : "not a shape";
      assert Link.parse("WR") == null : "a word with no letters is not sent";
      assert Link.parse("DR!!") == null : "a drawing with characters it never uses is not one";
      assert Link.parse("DR").drawing.equals(new DrawPad(() -> { }).text()) : "an empty drawing is one empty frame";
      System.out.println("LinkCheck passed");
   }
}
