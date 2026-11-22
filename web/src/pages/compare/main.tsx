import { hydrateRoot } from 'react-dom/client';
import { byId } from '../../shared/dom';
import { Compare } from './Compare';
import './compare.css';
import { registerAfterLoad } from '../../sw/register';

// The top bar and intro are written into compare.html (Intro.tsx), and the rest as React first renders it for the
// asked app (vite/pages.ts, compareRest), so the two players start loading as the page is read: this takes it over.
hydrateRoot(byId('compare-rest'), <Compare />);
registerAfterLoad();   // a visit that starts here plays offline later too (no boot.js on this page)
