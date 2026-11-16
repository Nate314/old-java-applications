// What a friend's link brings an app: the address parameter each app reads from its own page (<body data-args-page>,
// see the apps' READMEs). Pages that run an app in a frame (the home page's player, play.html) pass it on, so the
// link lands in the same state from any page it points at. Each app's row also says what a well-formed value starts
// with (value, matched right after 'name=') and the line its title screen shows for one (src/runtime/share.tsx).
// The runtime (boot.js) imports this file too, so vite.config.ts keeps it in a chunk of its own.
const LINKS: Record<string, { name: string; value: RegExp; invite: string }> = {
  CircleSpawner: { name: 'shape', value: /[CRHSWKD][RBS]/, invite: 'A friend sent you a shape. Press Play to see it built; your own shape stays saved.' },
  NumberGuess: { name: 'challenge', value: /[0-4][0-9a-z-]+(?:&|$)/i, invite: 'A friend sent you a number to find. Press Play and try to beat their guesses.' },
  UltimateTicTacToe: { name: 'game', value: /[a-i1-9]+(?:&|$)/i, invite: 'A friend sent you a game, and it is your move. Press Play to answer it.' },
  SynthesizerKeyboard: { name: 'tune', value: /(?:[a-m][1-8]){2,}(?:&|$)/i, invite: 'A friend sent you a tune. Press Play to hear it, then play it back.' },
  MusicTheoryExpert: { name: 'song', value: /[0-9ab][Mm][1-7]/, invite: 'A friend sent you a song. Press Play to hear it, change it, and keep it if you like it.' },
  WindowsE: { name: 'keys', value: /[a-z0-9]+(?:-[a-z0-9]+)*(?:&|$)/i, invite: 'A friend sent you a list of Windows shortcuts. Press Play, then Preview it before DO IT!!! presses them.' },
};

/** The app whose parameter the address carries (/?challenge=... is a NumberGuess link), or null. */
export function linkedApp(search: string): string | null {
  const params = new URLSearchParams(search);
  return Object.keys(LINKS).find(id => params.has(LINKS[id].name)) || null;
}

/** '&name=value' to add to app id's page address when the address carries its parameter, or ''. */
export function linkState(id: string, search: string): string {
  const name = LINKS[id]?.name, value = name && new URLSearchParams(search).get(name);
  return value ? '&' + name + '=' + encodeURIComponent(value) : '';
}

/** What the title screen says about the first well-formed friend's value the address carries, or ''. */
export function linkInvite(search: string): string {
  const link = Object.values(LINKS).find(l => new RegExp('[?&]' + l.name + '=' + l.value.source, l.value.flags).test(search));
  return link ? link.invite : '';
}
