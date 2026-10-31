# BibleDownloader

Downloads Bible chapters (NIV) from bible.com, either as each chapter's MP3 audio or as chapter text saved to HTML. Decompiled with Vineflower from the author's own `BibleDownloader1.3.1.jar` (jar file date 2016-08-11). Try-with-resources blocks the decompiler could not rebuild were rewritten by hand, and catch variables were renamed.

An older `AudioBibleDownloader1.1.jar` was not added: this 1.3.1 version is a later release of the same app (same `audiobibledownloader` package) that keeps the MP3 download and adds chapter text download.

## Controls / usage

- **Book**: book name, default `Luke`. It is lowercased into the page URL `https://www.bible.com/bible/111/<book>.<chapter>.niv.html`.
- **Chapters**: first and last chapter, default `1` to `24`. If the last is lower than the first, nothing happens. Non-numeric chapters show a message in the progress bar.
- **Audio**: for each chapter, finds the `.mp3` link in the page and saves it as `<Book><chapter>.mp3` (for example `Luke1.mp3`). A link without a scheme (`//host/...`) is fetched over https. The checkboxes do not apply.
- **Text**: extracts each chapter's text.
  - **Separate** unchecked (default): all chapters go into one `<Book>.html`, written after the last chapter.
  - **Separate** checked: one `<Book><chapter>.html` per chapter.
  - **Ch&V** checked (default): keeps bold chapter headings and verse numbers. Unchecked: verse text only.
- **Progress bar**: red bar that fills green with the percentage of chapters done. Next to the percentage it shows `done` at the end, or which chapters failed (a missing chapter, a network error, or a page whose markup did not match). A failed chapter does not stop the rest of the range.
- Downloads run on a background thread, one at a time: Audio and Text are disabled until the current download finishes. The window is about 330x152 px and not resizable. Title "BibleDownloader1.3.1".
- No command-line arguments.

## Code layout

- `src/audiobibledownloader/Main.java`: entry point and window; lays out the fields, buttons, checkboxes and the progress bar.
- `src/audiobibledownloader/DownloadManager.java`: one download of a chapter range; builds the chapter URL, picks the MP3 link or the verse text out of the page and writes the output files.
- `src/audiobibledownloader/WebpageDownloader.java`: reads a page into a string (as UTF-8), and copies a URL's response into a file.
- `src/audiobibledownloader/TextReader.java`: reads a whole text file into a string. Not used.
- `src/assets/bible.png`: the window icon.
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 BibleDownloader` (or `./run.ps1 BibleDownloader -Build` to build a jar).

The `.mp3` and `.html` files are written by relative path into the working directory (`BibleDownloader/`), overwriting files with the same name. HTML files are written as UTF-8 and say so in a `<meta charset>` tag. In one-file mode, `<Book>.html` is written after the last chapter with every chapter that succeeded. The icon is loaded from the classpath (`/assets/bible.png`), which `run.ps1` copies next to the compiled classes.

The text extraction splits the page on exact markup such as `<div class='columns large-6 reader-center' dir='ltr'` and `<span class="content">`, and the audio link is found by splitting on `.mp3` and `src='`. It was written against bible.com's 2016 page markup and may no longer work with the current site.

## Known quirks

- Only the HTML entities for curly quotes, the apostrophe and `&#8212;` (written as `--`) are converted in verse-only text. Other entities are left as they are, which a browser still displays correctly.
- The fields are laid out against the outer window size, so the right edge of the book field, the second chapter field and the checkboxes are slightly cut off.
