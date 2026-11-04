package stockchecker;

/** Dollar and percent text: "$10,234.50", "+$12.00", "-1.25%". */
final class Money {
   private Money() {
   }

   static String dollars(double value) {
      long cents = Math.round(Math.abs(value) * 100);
      return (value < 0 && cents > 0 ? "-$" : "$") + grouped(cents / 100) + "." + two(cents % 100);
   }

   /** Dollars with a sign in front, even for a gain. */
   static String change(double value) {
      String text = dollars(value);
      return text.startsWith("-") ? text : "+" + text;
   }

   /** The change from {@code from} to {@code to} as a signed percent with two decimals. */
   static String percent(double from, double to) {
      double ratio = from == 0 ? 0 : (to - from) / from;
      long hundredths = Math.round(Math.abs(ratio) * 10000);
      return (ratio < 0 && hundredths > 0 ? "-" : "+") + hundredths / 100 + "." + two(hundredths % 100) + "%";
   }

   private static String grouped(long whole) {
      String digits = Long.toString(whole);
      StringBuilder out = new StringBuilder();
      for (int i = 0; i < digits.length(); i++) {
         if (i > 0 && (digits.length() - i) % 3 == 0) {
            out.append(',');
         }
         out.append(digits.charAt(i));
      }
      return out.toString();
   }

   private static String two(long n) {
      return (n < 10 ? "0" : "") + n;
   }
}
