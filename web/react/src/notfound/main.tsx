// The 404 page's script. 404.html finds the site root first (it is served at any depth) and imports this from
// there, passing the root in window.oldjavaLostRoot.
import { mount } from '../shared/dom';
import { NotFound } from './NotFound';
import { LOST_APPS, destination } from './match';

declare global { interface Window { oldjavaLostRoot?: string } }

const base = window.oldjavaLostRoot || '/';
const icon = document.createElement('link');
icon.rel = 'icon';
icon.type = 'image/svg+xml';
icon.href = base + 'icons/icon.svg';
document.head.appendChild(icon);

let missing = location.pathname.slice(base.length);
try { missing = decodeURIComponent(missing); } catch (e) { /* keep it as typed */ }
const to = destination(LOST_APPS, missing.split('/'), location.search, location.hash);
if (to) {
  mount(<NotFound root={base} missing={missing} finding="Taking you there…" />);
  location.replace(base + to);
} else {
  document.title = 'Not found: /' + missing + ' - Old Java Applications';
  mount(<NotFound root={base} missing={missing.replace(/\/+$/, '')} finding={null} />);
}
