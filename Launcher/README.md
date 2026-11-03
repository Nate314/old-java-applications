# Launcher

The front door for the desktop: `./run.ps1` with no app name opens it. Every app in this repo is a card with its picture, category and how often you have played it, and Enter plays the selected one. It replaces the numbered list `run.ps1` used to print and ask about. Unlike the other folders, this is not a recovered app: it was written for this repo.

## Controls / usage

- **Cards**: one per app, all 20 in the web gallery's order. Each shows the app's thumbnail, its category, and "New: not launched here yet" (with a NEW badge) or "Played 3 times, last yesterday". An app that is running says so with a green RUNNING badge.
- **Search**: the field at the top right has the keyboard from the start. Typing narrows the cards to apps whose name, folder name, category or hint contain the text. **Esc** clears it.
- **Arrow keys** move the selection (Up and Down by a row; Left, Right, Home and End while the search field is empty). **Enter** plays the selected app, from anywhere in the window.
- **Filters**: All, Games, Music, Toys, Tools and Real keys (the apps that press real keys), each with its count.
- **Click** a card to select it, **double-click** to play it.
- **Detail pane** (right): a larger picture, the name, Decompiled jar or Original source, the hint, the play record, **Play** (Alt+P), **Read me** (Alt+R: the app's README in a window, Esc closes it) and **Open folder** (Alt+F).
- **Saves** (Alt+S): moves the apps' records between this computer and the web site. **Save a file...** writes every save file the apps keep in your home folder (`.minesweeper-best.txt`, `.clock.txt` and the rest), plus which apps you have launched, to `oldjava-saves.json` (in Downloads by default), for Your records > Load on the site. **Load a file...** reads a file from the site's Download (or from another computer), lists its apps with a box each (ticked where its records differ from the ones here) and writes the ticked apps' files over yours. Only the save files listed in `SaveBundle.java` are ever written, by name, into the home folder; the site's Then saves and its other files are left out. An app already running keeps its old records until it is started again.
- **Then and now** (Alt+T and Alt+N, above Play): Then plays each app as it was first restored, the same version the web site's Then pages run (commit `5a28e46`: the recovered apps with the light bug fixes of #6, before the reworks). In Then mode the picture well shows the app's lines of Java then and now as two bars, the hint is the first lines of its README as it was then, each card's bottom line reads like "450 lines then, 1,915 now", Play turns brown ("Play it as first restored") and Read me opens the README as it was then. An app whose code has not changed since (Windows, WindowsE) says so and Play runs today's. Today's and the first restored version of one app can run at the same time, side by side; the card says RUNNING THEN while the old one runs. The first restored Dino Game is the original screen bot, which presses real Space keys, so it carries a warning (taken from its old README) and asks first, like the Real keys apps. Then launches do not count in the play record, and Pick for me always plays today's version.
- **History** (Alt+H): every commit that changed the app's folder since it was first restored, newest first with dates, plus its lines of Java then and now.
- **Pick for me** (Alt+K): plays a random app you have not launched yet, from the cards shown (or from all, if none of those qualify). It never picks an app that presses real keys.
- **Apps that press real keys** (Windows, WindowsE) carry a Real keys chip, show a "Heads up" note and an orange Play button, and ask before they start.
- The launcher opens on the app you launched last, and stays open while apps run, so several can run at once. The same app cannot be started twice at once. The status line at the bottom says when an app starts, closes, or stops with an error (the details are in the console that ran `run.ps1`).
- The window is resizable (at least 780 x 620); the cards reflow into as many columns as fit.

## Where things come from

- **Names, hints, categories, origins and thumbnails** of all 20 apps are read from the gallery in `web/site/index.html` and the screenshots in `web/site/shots/`, so the home page stays the one place that describes an app. The gallery describes what Windows and Windows+E do on the site's pretend Windows, so their hints here, and their warnings, come from `Catalog.java`, written from their READMEs. An app without a screenshot would get a drawn card with its initials.
- **Playing an app** runs `run.ps1 <App>` in the PowerShell that started the launcher, from the repo root, so each app is compiled and started from its own working folder exactly as from the command line. Its output goes to the same console.
- **Then** comes from the repo's git history, so it needs git on the PATH and a clone with history back to `5a28e46` (without them the Now and Then buttons and History are hidden). The stories (lines of Java counted with `git grep`, the commits with `git log --no-merges`, the old READMEs with `git show`) are read in the background when the launcher opens. The first Then play unpacks that commit (everything but `web/`) with `git archive` into `out/then`, which is git-ignored, and runs that commit's own `run.ps1 <App>` there, so the old app is compiled and started exactly as it was then. Its home folder is `~/.oldjava-then` (set through `JAVA_TOOL_OPTIONS`, which is why the console prints "Picked up JAVA_TOOL_OPTIONS"), so the only first restored app that saves, Flappy Bird's high score, keeps its own file apart from today's. Delete `out/then` to unpack again.
- **The play record** (how many launches, and the last) is saved to `.oldjava-launcher.txt` in the user's home folder, one `App count lastLaunchMillis` line per app. It counts launches from the launcher only. Delete the file to start over.

## Code layout

- `src/launcher/Main.java`: entry point; reads the catalog from the current folder (the repo root) and opens the window. The optional argument is the PowerShell to run `run.ps1` with (default `powershell`).
- `src/launcher/Catalog.java`: the list of apps; parses the gallery and gives the two that press real keys their desktop hint and warning.
- `src/launcher/LauncherWindow.java`: the window: header with search and filters, the card grid, the detail pane, keys and launching.
- `src/launcher/SaveBundle.java`: the saves file shared with the web site: the save files each app keeps, reading them from and writing them to the home folder, and the JSON (with a small reader of its own).
- `src/launcher/SavesMenu.java`: the Saves button's dialogs and file choosers.
- `src/launcher/PlayLog.java`: the play record file, and its "Played twice, last yesterday" wording.
- `src/launcher/Runner.java`: starts an app in its own process (`run.ps1`) and reports its exit code; the Then runner unpacks the first restored code first and gives it its own home folder.
- `src/launcher/History.java`: each app's then and now from git: lines of Java, the commits since, its README as first restored, and the warning for a first restored app that presses keys.
- `src/launcher/ThenCopy.java`: unpacks the first restored commit into `out/then` once (refusing any path outside it).
- `src/launcher/Git.java`: runs git in the repo.
- `src/launcher/Thumbs.java`: loads and scales the thumbnails, and draws the cards for apps without one.
- `src/launcher/ReadmeView.java`: shows a README, turning a small part of Markdown into Swing HTML.
- `src/launcher/Ui.java`: colors, fonts, the flat buttons, the search field and the thin scroll bar.

## Running

From the repo root: `./run.ps1` (or `./run.ps1 Launcher`). It must run from the repo root, which `run.ps1` does.

## Self-check

`test/launcher/LauncherCheck.java` checks that the catalog has all 20 apps (each with a README, a `run.ps1` entry and a thumbnail), the two that press real keys, search, the play record file and its wording, the README conversion, and the saves file: each listed save file is named in its app's sources, a save and load keeps every byte, a file as the web site writes it loads, and files not listed (or hostile paths) are never written. It also checks Then: the commit matches `web/then/pom.xml`, every app has a first restored story, Minesweeper grew, only the first restored Dino Game warns, Windows is unchanged, and the unpacker refuses an archive path outside its folder (the git checks are skipped without history). From the repo root:

```
javac -d out/check/Launcher Launcher/src/launcher/*.java Launcher/test/launcher/*.java
java -ea -cp out/check/Launcher launcher.LauncherCheck
```
