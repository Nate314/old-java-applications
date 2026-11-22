// What an app's title card shows that both its live title screen (src/runtime/title-screen.tsx) and the early card in
// its page's HTML (src/pages/app/AppPage.tsx, EarlyTitle) need. Keep this file free of imports: the build reads it in
// Node, and outside src/shared, so boot.js does not pull in the pages' shared chunk for it.

/** The games that can play themselves, with the app's own key that starts its computer player (src/runtime/watch.tsx). */
export const WATCH: Record<string, string> = { Minesweeper: 'W', UltimateTicTacToe: 'W', TankGame: 'C', DinoGame: 'B' };

/** What an app's title card folds under its More line, named in its summary (title-screen.tsx; AppPage.tsx's early card). */
export function moreParts(app: { then?: boolean }, then: boolean) {
  return ['the source', app.then && (then ? 'today’s version' : 'the first version'), !then && 'the desktop jar', !then && 'its story']
    .filter(Boolean).join(', ');
}
