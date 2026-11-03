# The prompt behind the stacked UX chain

This repo's PRs from #10 onward were made by a chain of Claude Code subagents, one bold change per attempt, each PR stacked on the one before. The rules for that chain grew over the run as problems came up. Below is a single prompt that sets up the same chain from the start, with every rule that was learned along the way, for 100 attempts. Paste it into Claude Code from the repo root.

---

Run a chain of 100 attempts to improve this project. Each attempt is a fresh subagent given the same task, and each ships one pull request. Do not merge anything; I'll review and merge.

## The task every attempt gets (verbatim)

"Look at the current state of this project first. Then innovate: think about the user experience and think outside the box. Make one bold change that improves conversions or the look and feel, not a small tweak. Use any tool you have. You must ship a change every time."

## How to run it

1. **Write a shared brief file** in your scratchpad (not in the repo) and point every subagent at it, so prompts stay short. It holds the task above, the project context (the root README and web/README.md describe it), the rules below, the report format, and a running list of every PR so far with a one-line summary each. After every attempt, add its PR to the list and update the "known open issues" and "direction" notes.
2. **Rounds of three.** Each round runs three builder subagents at once, then one integrator:
   - **Lane A** picks one game or toy app (Minesweeper, UltimateTicTacToe, TankGame, FlappyBird, DinoGame, RubiksCube2D, SpriteFloatyStars, ScreenSaver, CircleSpawner, NathaNOS).
   - **Lane B** picks one tool or music app (Clock, SynthesizerKeyboard, MusicTheoryExpert, NumberGuess, SimpleAlgorithms, GroupCreator, StockChecker, BibleDownloader, Windows, WindowsE, the desktop Launcher).
   - **Lane C** works on the site itself (home page, app-page chrome, skins, the AWT/Swing shim, build scripts) and never edits an app's sources.
   - Builders each get their own git worktree, branch from the previous round's branch, commit locally, and don't push or open a PR. Lanes A and B touch only their app's folder and their app's own entries in shared files. Lane C makes only additive changes to `web/site/boot.js`, `index.html` and `shim.css` (no moving, renaming or reformatting) so merges stay clean. README edits go in new paragraphs or the app's own section.
   - The **integrator** works in my main checkout: it merges the three builder branches, resolves conflicts so all three features work, runs the self-checks, `node --check` and a full `mvn -f web/pom.xml package`, verifies all three features in Chrome, pushes one round branch and opens one PR based on the previous round's branch, with a section per feature.
   - After each round, remove the three worktrees and their local branches so the disk doesn't fill. Each builder counts as one attempt; finish with a single-builder round if 100 isn't a multiple of three.
3. **Ports.** Attempt N serves its build on port 8100 + N; the integrator uses its own. Never touch port 8000, which serves my checkout's `web/target/site`. Pass `-Port` to `web/shots.ps1`, `web/cards.ps1` and `web/clips.ps1`.
4. **Steer for variety.** Don't let attempts fall into one template. Earlier runs drifted into "levels + stars + saved record" for every game, then into one feature per app in a row; when you see a pattern, tell the next attempts to change scale or area (site-wide, cross-app, first impression, performance, accessibility, touch, fixing accumulated issues). Tools and toys should become useful or delightful in their own way, not gamified. A careful, well-tested change beats another panel.
5. **After each round,** give me a short plain-language note: what changed, what was verified, what wasn't, and anything that touched my machine.

## Rules for every subagent

- Follow my CLAUDE.md: no em-dashes in any prose (READMEs, commits, PR bodies), no invented facts in docs, keep the project dependency-free (run any npm tool from a scratch folder, via nvm), branch and PR via `gh`, never commit to master.
- Commit messages and PR bodies end with the attribution lines Claude Code gives you.
- Stage files by explicit path. Never `git add -A`/`.`, `commit -a`, `stash`, `reset --hard`, `clean`, and never commit `.claude/`.
- Verify in Chrome with the claude-in-chrome tools in a new tab: interact with the change, check at least a narrow (~400 px) and a medium (900x600) size, check all three skins (Modern, Classic, Arcade) when visuals change, and read the console. If an app has a self-check under `<App>/test/`, run it. If desktop Java changes, run the app on the desktop.
- Drive desktop windows from inside the JVM (dispatch events to components, render to images). Never send OS-level keystrokes or clicks unless the target window is confirmed in front.
- Never open the real microphone, camera, screen capture or location; use fake streams. If a test writes the system clipboard, save and restore it.
- `node --check` any site JS you edit before building. Run long commands (Maven, headless Chrome) in the background or with a timeout so you don't stall.
- Put scratch files in a new `link<N>-...` folder in the scratchpad, never in the repo or another attempt's folder.
- If you change how an app looks, regenerate that app's thumbnail, preview clip and share card with the scripts above (only for your app). Avoid committing large binaries without need.
- Clean up when done: stop your server and any Java or headless Chrome processes you started, unregister the service worker and clear caches and storage for your port, close your tab.
- Report back with: branch, PR URL (integrator only), title, a 2-3 sentence summary, what you verified, and what you couldn't.
