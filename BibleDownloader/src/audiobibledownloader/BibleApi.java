package audiobibledownloader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chapters from bible-api.com, a free service with public domain translations that a web page may also call (it
 * sends CORS headers). The bible.com pages the app was written for are gone. The service asks for at most 15
 * requests every 30 seconds, so requests are spaced at least {@link #SPACING_MS} apart, and chapters already
 * fetched are kept in memory.
 */
public final class BibleApi {
   public static final String[] TRANSLATIONS = {"web", "kjv", "asv", "bbe", "darby"};
   public static final String[] TRANSLATION_NAMES = {
      "World English Bible", "King James Version", "American Standard Version (1901)", "Bible in Basic English", "Darby Bible"
   };
   public static final long SPACING_MS = 2100;
   private static final int CACHE_SIZE = 40;

   private static long lastRequest;
   private static final Map<String, Chapter> cache = new LinkedHashMap<String, Chapter>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Chapter> eldest) {
         return this.size() > CACHE_SIZE;
      }
   };

   private BibleApi() {
   }

   public static String url(String translation, int book, int chapter) {
      return "https://bible-api.com/data/" + translation + "/" + Books.IDS[book] + "/" + chapter;
   }

   public static int translationIndex(String id) {
      for (int i = 0; i < TRANSLATIONS.length; i++) {
         if (TRANSLATIONS[i].equals(id)) {
            return i;
         }
      }
      return -1;
   }

   public static synchronized Chapter cached(String translation, int book, int chapter) {
      return cache.get(translation + "/" + book + "/" + chapter);
   }

   /** Blocks (call it off the event thread). Throws IOException with a short message for the reader to show. */
   public static Chapter fetch(String translation, int book, int chapter) throws IOException {
      Chapter hit = cached(translation, book, chapter);
      if (hit != null) {
         return hit;
      }
      String json;
      synchronized (BibleApi.class) {
         hit = cache.get(translation + "/" + book + "/" + chapter);
         if (hit != null) {
            return hit;
         }
         long wait = lastRequest + SPACING_MS - System.currentTimeMillis();
         if (wait > 0) {
            try {
               Thread.sleep(wait);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               throw new IOException("interrupted");
            }
         }
         lastRequest = System.currentTimeMillis();
         try {
            json = new WebpageDownloader().readHtmlPage(url(translation, book, chapter));
         } catch (FileNotFoundException e) {
            throw new IOException(Books.NAMES[book] + " " + chapter + " is not in this translation");
         } catch (IOException e) {
            String message = String.valueOf(e.getMessage());
            throw new IOException(message.contains("429") ? "bible-api.com is busy, try again in a moment" : "could not reach bible-api.com");
         }
      }
      Chapter parsed;
      try {
         parsed = Chapter.parse(book, chapter, translation, json);
      } catch (RuntimeException e) {
         throw new IOException("bible-api.com sent something unexpected");
      }
      synchronized (BibleApi.class) {
         cache.put(translation + "/" + book + "/" + chapter, parsed);
      }
      return parsed;
   }
}
