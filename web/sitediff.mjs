// Compares two built sites file by file, for a refactor's before and after, with Node's built-ins only (no npm
// packages). Content-hashed names (assets/<name>-<8 chars>.<ext>) and program stamps (?v=<hash>) change whenever any
// byte does, so files are paired by their name with the hash taken out, and text files are compared again with every
// hashed name and stamp replaced by its unhashed form. Each file is then one of: the same bytes, the same once names
// are mapped, different (size before -> after, and the first place it differs for text), only before, or only after.
// Jars are compared by their entries' names, CRCs and sizes, not their bytes (see entries below).
// Prints a summary and every file that is not the same; exits 1 when any file differs or is missing on one side.
// Build each version into its own folder first (for example -Dsite.dir, see the README), then from the repo root:
//   node web/sitediff.mjs C:\scratch\before C:\scratch\after
//   node web/sitediff.mjs C:\scratch\before web\target\site -All   (also list the files that are the same)
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';
import { gzipSync, inflateRawSync } from 'node:zlib';

const args = process.argv.slice(2), all = args.includes('-All'), dirs = args.filter(a => a !== '-All').map(d => resolve(d));
if (dirs.length !== 2 || !dirs.every(d => existsSync(join(d, 'index.html')))) {
  console.error('Usage: node web/sitediff.mjs <before site> <after site> [-All]   (each a built site with an index.html)');
  process.exit(2);
}
const HASHED = /^(assets\/.+)-[A-Za-z0-9_-]{8}(\.[a-z]+)$/;
const TEXT = /\.(html|js|mjs|css|json|txt|svg|webmanifest|xml|java|md)$/;
const keyOf = path => path.replace(HASHED, '$1-#$2');
const walk = (root, d = root) => readdirSync(d, { withFileTypes: true })
  .flatMap(e => e.isDirectory() ? walk(root, join(d, e.name)) : [relative(root, join(d, e.name)).split('\\').join('/')]);
const sites = dirs.map(root => ({ root, files: walk(root).sort() }));

// Every hashed file name on either side, so only real asset names are unhashed in text (not any word-dash-8-chars).
const hashed = new Set(sites.flatMap(s => s.files.filter(f => HASHED.test(f)).map(f => f.slice('assets/'.length))));
const unhash = text => text
  .replace(/[\w.-]+-[A-Za-z0-9_-]{8}\.[a-z]+/g, m => hashed.has(m) ? keyOf('assets/' + m).slice('assets/'.length) : m)
  .replace(/\?v=[A-Za-z0-9_-]+/g, '?v=#');

// Pair files by key; a key that several files share on one side pairs same-once-mapped files first, then in name order.
const groups = new Map();
sites.forEach((s, side) => s.files.forEach(f => {
  const g = groups.get(keyOf(f)) || [[], []];
  g[side].push(f);
  groups.set(keyOf(f), g);
}));
const cache = new Map(), once = (k, fn) => cache.has(k) ? cache.get(k) : cache.set(k, fn()).get(k);
const read = (side, f) => once(side + ':' + f, () => readFileSync(join(sites[side].root, f)));
// A jar's entries (name, CRC, size) from its central directory: Maven stamps each entry's time, so the bytes differ
// on every build. A jar inside it (NathaNOS carries the other apps' jars) is listed by its own entries the same way.
function entries(buf, prefix = '') {
  const end = buf.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  const out = [];
  for (let p = buf.readUInt32LE(end + 16), n = buf.readUInt16LE(end + 10); n--; p += 46 + buf.readUInt16LE(p + 28) + buf.readUInt16LE(p + 30) + buf.readUInt16LE(p + 32)) {
    const name = prefix + buf.toString('utf8', p + 46, p + 46 + buf.readUInt16LE(p + 28));
    if (!name.endsWith('.jar')) { out.push(name + ' ' + buf.readUInt32LE(p + 16) + ' ' + buf.readUInt32LE(p + 24)); continue; }
    const local = buf.readUInt32LE(p + 42), start = local + 30 + buf.readUInt16LE(local + 26) + buf.readUInt16LE(local + 28);
    const data = buf.subarray(start, start + buf.readUInt32LE(p + 20));
    out.push(entries(buf.readUInt16LE(p + 10) === 8 ? inflateRawSync(data) : data, name + '!/'));
  }
  return out.sort().join('\n');
}
const mapped = (side, f) => TEXT.test(f) ? once(side + '~' + f, () => unhash(read(side, f).toString('utf8')))
  : f.endsWith('.jar') ? once(side + '~' + f, () => entries(read(side, f))) : null;
const rows = [];
for (const [key, [before, after]] of [...groups].sort((a, b) => a[0] < b[0] ? -1 : 1)) {
  const kind = (b, a) => read(0, b).equals(read(1, a)) ? 'same' : mapped(0, b) !== null && mapped(0, b) === mapped(1, a) ? 'same once mapped' : 'differs';
  const leftB = [...before], leftA = [...after];
  for (const want of ['same', 'same once mapped']) for (const b of [...leftB]) {
    const i = leftA.findIndex(a => kind(b, a) === want);
    if (i >= 0) { rows.push({ key, b, a: leftA[i], kind: want }); leftA.splice(i, 1); leftB.splice(leftB.indexOf(b), 1); }
  }
  leftB.forEach((b, i) => rows.push(i < leftA.length ? { key, b, a: leftA[i], kind: 'differs' } : { key, b, kind: 'only before' }));
  leftA.slice(leftB.length).forEach(a => rows.push({ key, a, kind: 'only after' }));
}

const size = (side, f) => statSync(join(sites[side].root, f)).size;
const gz = (side, f) => gzipSync(read(side, f), { level: 9 }).length;
function firstDiff(b, a) {
  const x = mapped(0, b), y = mapped(1, a);
  let i = 0;
  while (i < x.length && x[i] === y[i]) i++;
  const line = x.slice(0, i).split('\n').length, cut = s => JSON.stringify(s.slice(Math.max(0, i - 40), i + 60));
  return `      line ${line}, char ${i}:\n        before ${cut(x)}\n        after  ${cut(y)}`;
}
const count = k => rows.filter(r => r.kind === k).length;
console.log(`before ${dirs[0]}: ${sites[0].files.length} files; after ${dirs[1]}: ${sites[1].files.length} files`);
for (const k of ['same', 'same once mapped', 'differs', 'only before', 'only after']) console.log(`  ${k}: ${count(k)}`);
for (const r of rows) {
  if (r.kind === 'same' && !all) continue;
  const name = r.a && r.b && r.a !== r.b ? `${r.b} -> ${r.a}` : r.a || r.b;
  if (r.kind === 'only before') console.log(`ONLY BEFORE ${name} (${size(0, r.b)} B)`);
  else if (r.kind === 'only after') console.log(`ONLY AFTER  ${name} (${size(1, r.a)} B)`);
  else if (r.kind === 'differs') {
    const d = size(1, r.a) - size(0, r.b), g = gz(1, r.a) - gz(0, r.b), sign = n => (n > 0 ? '+' : '') + n;
    console.log(`DIFFERS     ${name}: ${size(0, r.b)} -> ${size(1, r.a)} B (${sign(d)}), gzip ${sign(g)} B`);
    if (mapped(0, r.b) !== null) console.log(firstDiff(r.b, r.a));
  } else console.log(`${r.kind === 'same' ? 'SAME       ' : 'MAPPED     '} ${name}`);
}
if (rows.some(r => r.kind === 'differs' && /(classes|then)\.js$/.test(r.a)))
  console.log('Note: TeaVM renames some symbols on every build, so a few programs (classes.js, then.js) differ by a few bytes between two builds of the same sources; build "before" twice to see which.');
process.exit(count('differs') + count('only before') + count('only after') ? 1 : 0);
