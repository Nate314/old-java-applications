// What a friend's link brings an app: the address parameter each app reads from its own page (<body data-args-page>,
// see the apps' READMEs). Pages that run an app in a frame (the home page's player, play.html) pass it on, so the
// link lands in the same state from any page it points at.
const PARAMS: Record<string, string> = { CircleSpawner: 'shape', NumberGuess: 'challenge', UltimateTicTacToe: 'game', SynthesizerKeyboard: 'tune', MusicTheoryExpert: 'song', WindowsE: 'keys' };

/** The app whose parameter the address carries (/?challenge=... is a NumberGuess link), or null. */
export function linkedApp(search: string): string | null {
  const params = new URLSearchParams(search);
  return Object.keys(PARAMS).find(id => params.has(PARAMS[id])) || null;
}

/** '&name=value' to add to app id's page address when the address carries its parameter, or ''. */
export function linkState(id: string, search: string): string {
  const name = PARAMS[id], value = name && new URLSearchParams(search).get(name);
  return value ? '&' + name + '=' + encodeURIComponent(value) : '';
}
