// The home page's title and lead, its largest text, written into the page itself by the build (the home route's
// placeholder in src/routes.tsx), so a phone shows them before the scripts have run. No CSS import: the build reads
// this file in Node.
import apps from '../../shared/apps.json';

// The app the player starts with, as player.ts syncPop picks it, in a script: the page's own demo (Minesweeper), a
// #App link's app, or none (a Then link, the tour, a friend's ?link). Its shot is shots/<id>.webp, as webp() names it.
const START = '{let i=location.hash?location.hash.slice(1):location.search?"":"Minesweeper";if(' +
  JSON.stringify(apps.map(a => a.id)) + '.indexOf(i)>=0){';

/**
 * The player's poster (Home.tsx img.poster), the first screen's largest picture, asked for with the page, ahead of
 * the stylesheets: the early poster and the live one share its address, so it downloads once. High priority: on a
 * throttled phone the first paint comes ~0.15 s later but with the picture in it (LCP ~1.0 s; ~1.6 s at low or without).
 */
export function PosterPicture() {
  return <script dangerouslySetInnerHTML={{ __html: START + 'const l=document.createElement("link");l.rel="preload";l.as="image";' +
    'l.fetchPriority="high";l.href="shots/"+i+".webp";document.head.appendChild(l)}}' }} />;
}

/**
 * On a phone, the poster painted with the title, where the player's screen will be (React draws the player ~1.5 s
 * later on a slow phone, and the poster was the page's LCP), until early-poster.ts hands over to the live one.
 * Its box is the screen's: the player's width, its bottom 12 px above the window's (player.ts fit, first view).
 * Not for a visitor with stamps, whose Welcome back row moves the player down.
 */
export function EarlyPoster() {
  // shortcut: the start row and player bar are assumed one line and 134 px tall (190 px under the lead); where they
  // wrap (under ~380 px) the live poster lands up to ~55 px lower and early-poster.ts glides it there.
  return <script dangerouslySetInnerHTML={{ __html: START + 'let s=0;try{s=Object.keys(JSON.parse(localStorage.getItem("oldjava.played"))||{}).length}catch(e){}' +
    'if(!s&&matchMedia("(max-width: 640px)").matches){const p=document.querySelector("main p.lead"),t=p.getBoundingClientRect().bottom+190,' +
    'd=document.createElement("div");d.id="early-poster";d.style.top=t+scrollY+"px";d.style.height=Math.max(320,innerHeight-12-t)+"px";' +
    'const m=new Image();m.alt="";m.fetchPriority="high";m.src="shots/"+i+".webp";d.append(m);p.after(d)}}}' }} />;
}

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
