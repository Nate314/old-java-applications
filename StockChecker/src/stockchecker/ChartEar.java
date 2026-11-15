package stockchecker;

import java.util.List;
import javax.sound.midi.MidiChannel;

/**
 * Hear the chart: the picked stock's day so far played as notes in about three seconds (higher notes for higher
 * prices, on the chart's own scale, so the tune has the line's shape), with a chime at each of your trades; and, while
 * listening live, one short note per market minute. Steps on the desk's 40 ms timer. Silent without a synthesizer.
 */
final class ChartEar {
   static final int LOW_NOTE = 48;
   static final int HIGH_NOTE = 84;
   /** Timer ticks in one hearing: 75 x 40 ms, three seconds whatever the time of day. */
   static final int STEPS = 75;
   private static final int PIANO = 0;
   private static final int CHIME = 11;
   private static final int BUY_NOTE = 96;
   private static final int SELL_NOTE = 60;
   /** Ticks a live note sounds for. */
   private static final int LIVE_TICKS = 4;

   private MidiChannel line;
   private MidiChannel chime;
   private boolean noSynth;
   private String symbol;
   private int[] notes;
   private List<Portfolio.Trade> trades;
   private int now;
   private int step = -1;
   private int sounding = -1;
   private int chiming = -1;
   private int quietIn;
   boolean live;

   /** The MIDI note for {@code price} on a chart running from {@code low} to {@code low + span}. */
   static int note(double price, double low, double span) {
      double place = span <= 0 ? 0.5 : (price - low) / span;
      return LOW_NOTE + (int) Math.round(Math.max(0, Math.min(1, place)) * (HIGH_NOTE - LOW_NOTE));
   }

   /** The market minute heard at step {@code step} of a hearing of minutes 0 to {@code now}. */
   static int minuteAt(int step, int now) {
      return (int) Math.round((double) step * now / (STEPS - 1));
   }

   /** Starts hearing {@code stock}'s day from the open to minute {@code now}, on the chart's scale. */
   void hear(Market.Stock stock, int now, double low, double span, List<Portfolio.Trade> trades) {
      stop();
      this.symbol = stock.symbol;
      this.now = now;
      this.trades = trades;
      notes = new int[now + 1];
      for (int m = 0; m <= now; m++) {
         notes[m] = note(stock.path[m], low, span);
      }
      step = 0;
      play();
   }

   /** The stock being heard, or null. */
   String hearing() {
      return step < 0 ? null : symbol;
   }

   /** The market minute under the playhead, or -1. */
   int minute() {
      return step < 0 ? -1 : minuteAt(step, now);
   }

   void stop() {
      step = -1;
      quiet();
   }

   /** One timer tick: the next note of a hearing, or the end of a live note. */
   void tick() {
      if (step >= 0) {
         step++;
         if (step >= STEPS) {
            stop();
         } else {
            play();
         }
      } else if (quietIn > 0 && --quietIn == 0) {
         quiet();
      }
   }

   /** Listening live: a short note for this minute's price (not while a hearing plays). */
   void minute(double price, double low, double span) {
      if (!live || step >= 0) {
         return;
      }
      sound(note(price, low, span), 70);
      quietIn = LIVE_TICKS;
   }

   private void play() {
      int minute = minuteAt(step, now);
      sound(notes[minute], 90);
      // A chime for each of your trades this step passes over.
      int from = step == 0 ? -1 : minuteAt(step - 1, now);
      for (Portfolio.Trade trade : trades) {
         if (trade.symbol.equals(symbol) && trade.minute > from && trade.minute <= minute && open()) {
            if (chiming >= 0) {
               chime.noteOff(chiming);
            }
            chiming = trade.shares > 0 ? BUY_NOTE : SELL_NOTE;
            chime.noteOn(chiming, 110);
         }
      }
   }

   /** Sounds {@code note} on the line's channel, keeping a held note held when it does not change. */
   private void sound(int note, int velocity) {
      if (note == sounding || !open()) {
         return;
      }
      if (sounding >= 0) {
         line.noteOff(sounding);
      }
      sounding = note;
      line.noteOn(note, velocity);
   }

   private void quiet() {
      if (sounding >= 0 && line != null) {
         line.noteOff(sounding);
      }
      if (chiming >= 0 && chime != null) {
         chime.noteOff(chiming);
      }
      sounding = -1;
      chiming = -1;
      quietIn = 0;
   }

   private boolean open() {
      if (line == null && !noSynth) {
         line = Bell.channel(1, PIANO);
         chime = Bell.channel(2, CHIME);
         noSynth = line == null || chime == null;
      }
      return !noSynth;
   }
}
