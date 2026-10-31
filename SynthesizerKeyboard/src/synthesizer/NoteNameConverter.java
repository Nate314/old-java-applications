package synthesizer;

public final class NoteNameConverter {
   public static int noteNumber(String noteName) {
      int result = 0;
      String[] stepNames = new String[]{"A", "As", "B", "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs"};

      for (int octave = -30; octave < 31; octave++) {
         for (int step = 0; step < 12; step++) {
            String candidate = stepNames[step] + (octave < 0 ? "n" + Math.abs(octave) : String.valueOf(octave));
            if (noteName == candidate) {
               result = 12 + octave * 12 + step - 3;
            }
         }
      }

      return result;
   }
}
