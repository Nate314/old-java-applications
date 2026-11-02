# BibleDownloader

Once a downloader for Bible chapters (NIV) from bible.com, as MP3 audio or as chapter text saved to HTML. Those bible.com pages are gone (their 2016 addresses return 404), so it is now a Bible reader: an open book on a leather desk that fetches chapters from [bible-api.com](https://bible-api.com), sets them as printed pages, and turns into the next chapter when you turn past the last page. It still saves chapters as web pages, the job it was written for.

Decompiled with Vineflower from the author's own `BibleDownloader1.3.1.jar` (jar file date 2016-08-11). An older `AudioBibleDownloader1.1.jar` was not added: 1.3.1 is a later release of the same app (same `audiobibledownloader` package) that keeps the MP3 download and adds chapter text download.

## Reading

- **The book**: two pages side by side, or one page at a time when the window is narrower than 700 px. Each book's first chapter opens with its title; every chapter starts with a large red chapter number, and verse numbers are small and red, as in a printed Bible. The running head shows the verses on the page ("LUKE 1:6-14") and the translation; the foot shows the page number. A red ribbon marks the book.
- **Turning pages**: Right, Page Down or Space turns forward; Left, Page Up or Backspace turns back. Clicking the left half of the book turns back and the right half forward, as do the Prev and Next buttons. Past the last page comes the next chapter (and from Malachi to Matthew, on to Revelation 22); back from the first page goes to the previous chapter's last pages. N and P jump a whole chapter; Home goes to the chapter's first page.
- **Highlights**: hovering a verse tints it; clicking it highlights it in yellow, and clicking again takes the highlight off. M opens the next highlighted verse, wrapping round to the first one. Highlights belong to the verse, so they show in every translation.
- **Chapters read**: a chapter counts as read once its last page is open. A message says so ("Luke 1 read: 1 of 1,189 chapters").
- **Books** (B): the contents. The Old Testament and the New Testament, each book with a thin bar of how much of it has been read (gold when finished), the total of chapters read and the number of highlights. Click a book to see its chapters as numbered squares, filled red when read and ringed in gold for the open one; click one to open it, or use Start reading / Continue at N. Esc goes back.
- **Translation** (T, or the button showing `WEB`): World English Bible (the default), King James Version, American Standard Version (1901), Bible in Basic English and Darby Bible, all public domain. The open chapter reloads in the new translation at the same place.
- **Text size**: A- and A+ (or - and +) switch between three sizes; the pages are set again and the place is kept.
- The window is resizable on the desktop; the pages are set again for the new size.

## Saving chapters

Save (S) opens the save view for the open book; a book's chapter view has "Save as a web page" too.

- **From chapter / To chapter**: the range, with - and + buttons, within one book.
- **One file for all the chapters** (checked by default): one `<Book>.html` (for example `Luke.html`, `1John.html`). Unchecked: one `<Book><chapter>.html` per chapter, as the original's Separate checkbox did.
- **Chapter headings and verse numbers** (checked by default): each chapter gets a heading and bold verse numbers. Unchecked: verse text only, as the original's Ch&V checkbox did.
- **Save N chapters**: fetches the chapters one by one and fills the original red and green progress bar. The status line says what was saved, and which chapters failed and why.
- Files go to `~/Downloads` (created if missing), overwriting files with the same name. Each is UTF-8 HTML with a readable style and the translation's name at the end.
- The MP3 audio download is gone: bible-api.com has text only.

## bible-api.com

Chapters come from `https://bible-api.com/data/<translation>/<BOOK>/<chapter>` (for example `https://bible-api.com/data/web/LUK/1`), a free service by Tim Morgan. Its terms ask for at most 15 requests every 30 seconds per address and not to download a whole Bible, so:

- requests are spaced at least 2.1 seconds apart (`BibleApi.SPACING_MS`), so saving Luke's 24 chapters takes under a minute;
- the last 40 chapters fetched are kept in memory, and the next chapter is fetched in the background while you read, so turning into it is instant;
- a failed request shows the reason on the page ("could not reach bible-api.com", or "bible-api.com is busy, try again in a moment" for HTTP 429) with Try again (or Enter).

The chapter counts of the 66 books in `Books.java` were checked against the service's `/data/web/<BOOK>` lists.

## What it remembers

`~/.bibledownloader.txt` holds one `key=value` line each: `translation`, `place` (readable, for example `Luke 1:14`), `at` (book id, chapter and the verse at the top of the open page), `size`, `read` (every chapter read, as `LUK.1,LUK.2`) and `marks` (highlighted verses, as `JHN.3.16`). A damaged line is skipped. The first run opens at Luke 1, the app's old default book.

## Code layout

- `src/audiobibledownloader/Main.java`: entry point and window; sizes the window to the screen (at most 900 x 600).
- `src/audiobibledownloader/ReaderPanel.java`: the book, the contents, the chapters and save views, the toolbar, the keys and the mouse. Everything is drawn, and clickable areas are recorded while painting.
- `src/audiobibledownloader/Layout.java`: sets a chapter as pages (title, chapter numeral, verse numbers, word wrap) for a page width and line count.
- `src/audiobibledownloader/Chapter.java`: one chapter's verses, and a small reader for the service's JSON.
- `src/audiobibledownloader/BibleApi.java`: chapter addresses, the translations, request spacing and the in-memory cache.
- `src/audiobibledownloader/Books.java`: the 66 books' ids, names and chapter counts.
- `src/audiobibledownloader/Shelf.java`: what is remembered, and the file it lives in.
- `src/audiobibledownloader/DownloadManager.java`: saving a chapter range as HTML on its own thread (the original class, now fetching through `BibleApi`).
- `src/audiobibledownloader/WebpageDownloader.java`: reads a URL into a string as UTF-8 (the original class; its old Internet Explorer User-Agent header is gone, since a browser refuses to set it).
- `src/audiobibledownloader/TextReader.java`: reads a whole text file into a string. Not used.
- `src/assets/bible.png`: the window icon (also NathaNOS's icon for the app).
- `src/META-INF/MANIFEST.MF`: the manifest from the original jar.

## Running

From the repo root: `./run.ps1 BibleDownloader` (or `./run.ps1 BibleDownloader -Build` to build a jar). It needs an internet connection to fetch chapters.

The icon is loaded from the classpath (`/assets/bible.png`), which `run.ps1` copies next to the compiled classes. In the browser build there is no class path, so the window has no icon there.

## Self-check

`test/audiobibledownloader/ReaderCheck.java` checks the book table (66 books, 1,189 chapters), the JSON reader, the page layout (every word placed once, inside the page), the remembered file across a reload and with damaged lines, and the saved HTML. It needs no connection. From the repo root:

```
javac -encoding UTF-8 -d out/check/BibleDownloader BibleDownloader/src/audiobibledownloader/*.java BibleDownloader/test/audiobibledownloader/*.java
java -ea -cp out/check/BibleDownloader audiobibledownloader.ReaderCheck
```
