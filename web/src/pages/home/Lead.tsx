// The home page's title and lead, its largest text, written into the page itself by the build (the home route's
// placeholder in src/routes.tsx), so a phone shows them before the scripts have run. No CSS import: the build reads
// this file in Node.
import apps from '../../shared/apps.json';

export function Lead() {
  return (
    <>
      <h1>Old Java Applications</h1>
      <p className="lead">
        <strong>{apps.length} games, music toys and tools</strong> from old Java jars, running in your browser.
        {' '}One is already playing below: click it to take over.
        <span className="tech">
          {' '}They are Swing and AWT programs compiled to JavaScript with TeaVM, on a small AWT/Swing layer that draws the original windows and widgets.
        </span>
      </p>
    </>
  );
}
