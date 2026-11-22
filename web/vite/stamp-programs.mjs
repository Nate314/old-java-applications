// The app pages' programs get their build's stamp: once Maven has compiled every app (this runs last, from
// web/stamps/pom.xml), each <App>/index.html and then.html loads its classes.js or then.js as ?v=<hash of its content>,
// so no cache pairs a page with another build's program. The files keep their names, so the service worker's cache
// key (which leaves out the query), Save all for offline and anything linking them stay as they were.
// Usage: node web/vite/stamp-programs.mjs <site dir>
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { Script } from 'node:vm';

const site = process.argv[2];
if (!site || !existsSync(site)) throw new Error('Usage: node stamp-programs.mjs <site dir>');
let stamped = 0;
for (const dir of readdirSync(site, { withFileTypes: true }).filter(d => d.isDirectory())) {
  for (const [page, program] of [['index.html', 'classes.js'], ['then.html', 'then.js']]) {
    const html = join(site, dir.name, page), js = join(site, dir.name, program);
    if (!existsSync(html) || !existsSync(js)) continue;
    const code = readFileSync(js);
    // Fail the build on a program cut off part way, or one with next to nothing in it (round 67's build race wrote a
    // whole 9.6 KB Minesweeper). shortcut: 20 KB floor, as the smallest real program (with the shim) is about 38 KB.
    try { new Script(code.toString('utf8'), { filename: js }); } catch (e) { throw new Error(js + ' is not a whole program (' + e.message + '); rerun mvn clean package'); }
    if (code.length < 20000) throw new Error(js + ' is only ' + code.length + ' bytes, too small for an app; rerun mvn clean package');
    const v = createHash('sha256').update(code).digest('base64url').slice(0, 8);
    const text = readFileSync(html, 'utf8');
    const out = text.replace(new RegExp(' src="' + program.replace('.', '\\.') + '(\\?v=[^"]*)?"'), ' src="' + program + '?v=' + v + '"');
    if (!out.includes(' src="' + program + '?v=' + v + '"')) throw new Error(html + ' does not load ' + program);
    if (out !== text) writeFileSync(html, out);
    stamped++;
  }
}
console.log('Stamped ' + stamped + ' app programs with their content hash');
