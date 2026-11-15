import { mount } from '../../shared/dom';
import { Home } from './Home';
import { initPlayer } from './player';
import { initTakeaway } from './takeaway';
import { initClips } from './clips';
import { initArcadeTabs } from './tabs';
import { initNews } from './news';

// In the order the page's scripts ran when it was static HTML; What's new follows (it was a deferred news.js).
mount(<Home />);
initPlayer();
initTakeaway();
initClips();
initArcadeTabs();
initNews();
