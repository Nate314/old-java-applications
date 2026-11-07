import { SKINS, setSkin } from './skin';
import { bandNews, news, setNews, setZoomFit, showZoom, stageEdge, zoomFit } from './band-news';
import { popOut, poppable, showPop } from './pop-out';
import { replayNow } from './replay';
import { snapPhoto, takePhoto } from './photo-booth';
import { pad, padOnPage } from './touch-pad';
import { appId, catalog } from './catalog';
import { eraSwitch, indexOf, openDrawer, pageOf } from './then-and-now';
import { openShare } from './share';
import { howToPlay } from './source-viewer';
import { setSoundOn, showSound, soundOn } from './sounds';

export let siteRoot: string;
let theme: HTMLMetaElement;

function headLink(rel: string, href: string, type?: string) {
  var link = document.createElement('link');
  link.rel = rel;
  link.href = siteRoot + href;
  if (type) link.type = type;
  document.head.appendChild(link);
}

export function toolbar(isApp: boolean) {
  var bar = document.createElement('div');
  bar.className = 'shim-toolbar';
  if (isApp) {
    // The name's slot doubles as the band's news line (bandNews): How to play and new stamps show there.
    var slot = document.createElement('div');
    slot.className = 'shim-bar-slot';
    var name = document.createElement('strong');
    name.className = 'shim-bar-name';
    name.textContent = document.title;
    slot.appendChild(name);
    bar.appendChild(slot);
    setNews(bandNews(slot));
    var help = document.createElement('button');
    help.type = 'button';
    help.className = 'shim-help';
    help.title = 'How to play: show the hint in this band';
    help.setAttribute('aria-label', 'How to play');
    help.setAttribute('aria-pressed', 'false');
    help.textContent = '?';
    help.addEventListener('click', function () {
      if (news!.kind() === 'hint') news!.clear(); else howToPlay();
      help.blur();
    });
    news!.onKind(function (kind: string | null) { help.setAttribute('aria-pressed', String(kind === 'hint')); });
    bar.appendChild(help);
    bar.appendChild(eraSwitch('shim-era'));
    var back = document.createElement('a');
    back.href = '../index.html';
    back.textContent = 'All apps';
    bar.appendChild(back);
    var switcher = document.createElement('button');
    switcher.type = 'button';
    switcher.textContent = 'Switch app';
    switcher.addEventListener('click', function () { openDrawer(switcher); });
    bar.appendChild(switcher);
    var source = document.createElement('a');
    source.href = '#source';
    source.className = 'shim-source-open';
    source.title = 'Read the Java source of this app while it runs';
    source.setAttribute('data-sound', 'none');   // the panel plays its own open sound
    source.innerHTML = '&lt;/&gt;<span> Source</span>';
    bar.appendChild(source);
    var share = document.createElement('button');
    share.type = 'button';
    share.className = 'shim-share-open';
    share.title = 'Share this app: its link, with a preview card';
    share.setAttribute('data-sound', 'none');   // the panel plays its own open sound
    share.innerHTML = '↗<span> Share</span>';
    share.addEventListener('click', function () { openShare(appId(), share); });
    bar.appendChild(share);
    var instant = document.createElement('button');   // shown once the app runs and replay is recording
    instant.type = 'button';
    instant.className = 'shim-replay';
    instant.title = 'Instant replay: the last 10 to 20 seconds of play, as a video to watch, save or share';
    instant.setAttribute('aria-label', 'Instant replay');
    instant.setAttribute('data-sound', 'none');   // the panel plays its own open sound
    instant.innerHTML = '<span class="shim-replay-dot" aria-hidden="true"></span><span> Replay</span>';
    instant.addEventListener('click', function () { replayNow(instant); });
    bar.appendChild(instant);
    var camera = document.createElement('button');   // shown once the app runs (data-photo)
    camera.type = 'button';
    camera.className = 'shim-photo-open';
    camera.title = 'Take a photo of the app: a picture to save, copy or share, in an instant print or postcard frame';
    camera.setAttribute('aria-label', 'Take a photo');
    camera.setAttribute('data-sound', 'none');   // the shutter is its sound
    camera.innerHTML = '<span aria-hidden="true">&#128247;</span><span> Photo</span>';
    camera.addEventListener('click', function () { snapPhoto(takePhoto, document.getElementById('desktop'), camera); });
    bar.appendChild(camera);
    var controls = null;
    if (pad.has(appId()!)) {
      controls = document.createElement('button');
      controls.type = 'button';
      controls.textContent = 'Controls';
      controls.className = 'shim-controls-open';
      controls.title = 'Show or hide the on-screen controller';
      bar.appendChild(controls);
    }
    var next = document.createElement('a');
    next.className = 'shim-next';
    next.hidden = true;
    bar.appendChild(next);
    catalog().then(function (apps: OldjavaCatalogApp[]) {
      var i = indexOf(apps, appId());
      if (i < 0) return;
      var n = apps[(i + 1) % apps.length];
      next.href = pageOf(n);
      next.textContent = 'Next: ' + n.name + ' \u203a';
      next.hidden = false;
    });
  }
  if (isApp && !document.body.hasAttribute('data-own-layout')) {
    var zoom = document.createElement('button');
    zoom.type = 'button';
    zoom.className = 'shim-zoom';
    zoom.addEventListener('click', function () { setZoomFit(!zoomFit()); zoom.blur(); });
    bar.appendChild(zoom);
  }
  if (isApp && poppable() && window.top === window) {   // only a top-level page may open the window
    var pop = document.createElement('button');
    pop.type = 'button';
    pop.className = 'shim-pop';
    pop.setAttribute('data-sound', 'none');   // popOut and bringBack play their own
    pop.addEventListener('click', function () { popOut(); pop.blur(); });
    bar.appendChild(pop);
  }
  var sound = document.createElement('button');
  sound.type = 'button';
  sound.className = 'shim-sound';
  sound.setAttribute('data-sound', 'none');
  sound.addEventListener('click', function () { setSoundOn(!soundOn()); sound.blur(); });
  bar.appendChild(sound);
  var label = document.createElement('label');
  label.textContent = 'Skin ';
  var select = document.createElement('select');
  SKINS.forEach(function (o) {
    var opt = document.createElement('option');
    opt.value = o[0];
    opt.textContent = o[1];
    select.appendChild(opt);
  });
  select.value = document.documentElement.getAttribute('data-skin')!;
  select.addEventListener('change', function () { setSkin(select.value); select.blur(); });
  label.appendChild(select);
  bar.appendChild(label);
  document.body.appendChild(bar);
  if (isApp) stageEdge(bar, '--bar-h');
  showSound();
  showZoom();
  showPop();
  if (controls) padOnPage(controls);
}

export function init() {
  // ---- installable and offline: the web app manifest, icons and the service worker (sw.js) ----
  // Every page links them, so the arcade can be installed from any page and every page can be kept offline.
  siteRoot = new URL(/* @vite-ignore */ '..', import.meta.url).href;   // this module is assets/boot.js
  headLink('manifest', 'manifest.webmanifest');
  headLink('icon', 'icons/icon.svg', 'image/svg+xml');
  headLink('apple-touch-icon', 'icons/apple-touch-icon.png');
  theme = document.createElement('meta');
  theme.name = 'theme-color';
  theme.content = '#170a2e';
  document.head.appendChild(theme);
  if ('serviceWorker' in navigator && window.isSecureContext) {
    navigator.serviceWorker.register(siteRoot + 'sw.js').catch(function (e) {
      console.warn('Offline play is not available: ' + e.message);
    });
  }
  // The home page shows its own Install button, so it keeps the browser's install prompt for that button.
  window.addEventListener('beforeinstallprompt', function (e) {
    if (document.body && document.body.hasAttribute('data-app')) return;
    e.preventDefault();
    window.oldjavaInstall = e as Event & OldjavaInstallPrompt;
    document.dispatchEvent(new Event('oldjava-installable'));
  });
}
