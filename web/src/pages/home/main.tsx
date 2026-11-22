import { mount } from '../../shared/dom';
import { Home } from './Home';
import { initPlayer } from './player';
import { initTakeaway } from './takeaway';
import { initClips } from './clips';
import { initArcadeTabs } from './tabs';
import { initNews } from './news';
import { handOverPoster } from './early-poster';

// In the order the page's scripts ran when it was static HTML; What's new follows (it was a deferred news.js).
mount(<Home />, 'home-root');   // in the page's <main>, after its title and lead (src/routes.tsx)
initPlayer();
handOverPoster();   // the HTML's phone poster gives way to the player's (Lead.tsx EarlyPoster)
initTakeaway();
initClips();
initArcadeTabs();
initNews();
