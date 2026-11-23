// Checks that every app in src/shared/apps.json is wired into each place an app must be, with Node's built-ins only.
// Since round 66 a new app needs an entry in several files, and lists kept by hand drift. For each app: its folder,
// thumbnail (png, webp, 240 px webp), share card, preview clip exactly when "clip" is set, one TeaVM compile (an
// apps-js part, or nathanos for NathaNOS), one Then compile exactly when "then" is set, its Source viewer copy
// (apps/pom.xml) and its jar (jars/pom.xml). For NathaNOS's desktop: the jars nathanos/pom.xml makes, the jars
// AppPage.tsx gives NathaNOS's page (and the Then pair), every other assets/ file that page names, and the start
// menu kind (StartMenu.KIND, which must match apps.json "cat") and icon card line (IconTip.ABOUT) of each app.
// Prints every mismatch and exits 1 on any. Runs first in npm run build:pages; by hand: node web/wiring.mjs
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const web = dirname(fileURLToPath(import.meta.url)), repo = join(web, '..');
const read = path => readFileSync(join(repo, path), 'utf8');
const all = (text, re) => [...text.matchAll(re)].map(m => m[1]);
const problems = [];
const expect = (ok, what) => ok || problems.push(what);
const sameSet = (a, b, what) => {
  for (const x of a) expect(b.includes(x), `${what}: ${x} only in the first list`);
  for (const x of b) expect(a.includes(x), `${what}: ${x} only in the second list`);
};

const apps = JSON.parse(read('web/src/shared/apps.json'));
const ids = apps.map(a => a.id);

// TeaVM compiles: the app a compile is for is the folder it writes into.
const compiles = dir => readdirSync(join(web, dir), { withFileTypes: true }).filter(e => e.isDirectory())
  .flatMap(e => all(read(`web/${dir}/${e.name}/pom.xml`), /<targetDirectory>\$\{site\.dir\}\/(\w+)<\/targetDirectory>/g));
const programs = [...compiles('apps-js'), ...all(read('web/nathanos/pom.xml'), /<targetDirectory>\$\{site\.dir\}\/(\w+)</g)];
const thens = [...compiles('then-js'), ...all(read('web/then-nathanos/pom.xml'), /<targetDirectory>\$\{site\.dir\}\/(\w+)</g)];
const sources = all(read('web/apps/pom.xml'), /<appsource app="(\w+)"/g);
const jars = all(read('web/jars/pom.xml'), /<plainjar app="(\w+)"/g);
const count = (list, id) => list.filter(x => x === id).length;

for (const app of apps) {
  const { id } = app;
  expect(existsSync(join(repo, id, 'src')), `${id}: no ${id}/src folder`);
  expect(['game', 'music', 'toy', 'tool'].includes(app.cat), `${id}: cat "${app.cat}" is not game, music, toy or tool`);
  expect(app.shot === `shots/${id}.png`, `${id}: shot is "${app.shot}", not shots/${id}.png`);
  for (const f of [`shots/${id}.png`, `shots/${id}.webp`, `shots/${id}-240.webp`, `cards/${id}.jpg`]) {
    expect(existsSync(join(web, 'public', f)), `${id}: no web/public/${f} (shots.ps1, cards.ps1)`);
  }
  expect(!!app.clip === existsSync(join(web, 'public/clips', id + '.mp4')),
    `${id}: "clip" is ${!!app.clip} but web/public/clips/${id}.mp4 ${app.clip ? 'is missing' : 'exists'}`);
  expect(count(programs, id) === 1, `${id}: ${count(programs, id)} TeaVM compiles in apps-js/part*/ or nathanos/, not 1`);
  expect(count(thens, id) === (app.then ? 1 : 0), `${id}: "then" is ${!!app.then} but ${count(thens, id)} Then compiles`);
  expect(count(sources, id) === 1, `${id}: ${count(sources, id)} <appsource> lines in web/apps/pom.xml, not 1`);
  expect(count(jars, id) === 1, `${id}: ${count(jars, id)} <plainjar> lines in web/jars/pom.xml, not 1`);
}
for (const id of new Set([...programs, ...thens])) expect(ids.includes(id), `${id}: compiled but not in apps.json`);

// NathaNOS's desktop. BODY's NathaNOS entry comes first in AppPage.tsx, THEN_BODY's second.
const appPage = read('web/src/pages/app/AppPage.tsx');
const [now, then] = [...appPage.matchAll(/NathaNOS: \{\s*'data-files': \[([^\]]*)\]/g)].map(m => m[1]);
// A jar is made by an <appjar> line or kept in NathaNOS/assets (Clock.jar, which both pages copy).
const kept = readdirSync(join(repo, 'NathaNOS/assets')).filter(f => f.endsWith('.jar')).map(f => f.slice(0, -4));
const jarsOf = pom => [...new Set([...all(read(pom), /<appjar app="(\w+)"/g), ...kept])];
const desktop = jarsOf('web/nathanos/pom.xml');
sameSet(desktop, all(now, /assets\/(\w+)\.jar/g), 'NathaNOS jars, web/nathanos/pom.xml vs AppPage.tsx BODY');
// The Then page runs the apps then-nathanos compiles, with the jars web/nathanos writes (both pages share NathaNOS/assets).
const thenApps = all(read('web/then-nathanos/pom.xml'), /<source>\$\{then\.src\}\/(\w+)\/src<\/source>/g).filter(a => a !== 'NathaNOS');
sameSet([...new Set([...thenApps, ...kept])], all(then, /assets\/(\w+)\.jar/g),
  'NathaNOS Then jars, web/then-nathanos/pom.xml sources vs AppPage.tsx THEN_BODY');
for (const a of thenApps) expect(desktop.includes(a), `${a}: NathaNOS Then runs it, but web/nathanos/pom.xml writes no ${a}.jar`);
for (const f of all(now, /assets\/(\w+\.(?!jar)\w+)/g)) {
  expect(existsSync(join(repo, 'NathaNOS/assets', f)), `NathaNOS: AppPage.tsx names assets/${f}, not in NathaNOS/assets`);
}

const menu = read('NathaNOS/src/main/StartMenu.java');
const kind = new Map([...menu.matchAll(/KIND\.put\("(\w+)", "(\w+)"\)/g)].map(m => [m[1], m[2]]));
for (const [, list, k] of menu.matchAll(/new String\[\] \{([^}]*)\}\) \{\s*KIND\.put\(\w+, "(\w+)"\)/g)) {
  for (const id of all(list, /"(\w+)"/g)) kind.set(id, k);
}
const about = all(read('NathaNOS/src/main/IconTip.java'), /ABOUT\.put\("(\w+)",/g);
const KINDS = { game: 'Games', music: 'Music', toy: 'Toys', tool: 'Tools' };
for (const id of desktop) {
  const app = apps.find(a => a.id === id);
  expect(!!app, `${id}: on NathaNOS's desktop but not in apps.json`);
  if (app) expect(kind.get(id) === KINDS[app.cat], `${id}: StartMenu.KIND says ${kind.get(id) ?? "nothing (Other)"}, apps.json cat "${app.cat}"`);
  expect(about.includes(id), `${id}: no IconTip.ABOUT line`);
}
// Paint is NathaNOS's own (Launcher.PAINT), not a jar: its entries use the constant, so the patterns above skip it.
for (const id of new Set([...kind.keys(), ...about])) expect(desktop.includes(id), `${id}: in StartMenu.KIND or IconTip.ABOUT but not on the desktop`);

for (const p of problems) console.log('FAIL ' + p);
console.log(`wiring: ${apps.length} apps, ${desktop.length} on NathaNOS's desktop, ${problems.length ? problems.length + ' mismatches' : 'all in step'}`);
process.exit(problems.length ? 1 : 0);
