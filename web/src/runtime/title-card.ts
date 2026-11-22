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

/** A Then page's hint (then.html), where today's page has the app's own. */
export const thenHint = (app: { origin?: string }) => (app.origin === 'original' ? 'The author’s own source as it was found' : 'The code as it came out of the old jar') +
  ', with the light bug fixes of its 2026 restoration, before the later reworks.';

// #6 fixed the quirks the READMEs listed before it, so most then.html pages have none to list.
export const NO_QUIRKS = 'None left: its README at that commit lists no known quirks.';

/** The "## Known quirks" list of a README as first restored: one entry per bullet, as [text, code, text, ...]. */
export function quirkItems(readme: string): string[][] {
  const section = /^## Known quirks[^\n]*\n([\s\S]*?)(?=^## |(?![\s\S]))/m.exec(readme.replace(/\r\n?/g, '\n'));
  return section ? section[1].split(/^- /m).slice(1).map(item => item.replace(/\*\*/g, '').replace(/\s+/g, ' ').trim().split('`')) : [];
}
