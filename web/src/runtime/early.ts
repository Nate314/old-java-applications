// The head script of every page that runs the site's runtime (the home page, the app pages and their Then pages),
// inlined there by vite/pages.ts so it runs before first paint: the visitor's skin, Your skin's colors, and the
// attributes the stage's CSS reads from the address. The runtime itself (main.ts) is a module, run once the page
// is parsed. See paintSkin in skin.ts.
import { paintSkin } from './skin';

paintSkin();
