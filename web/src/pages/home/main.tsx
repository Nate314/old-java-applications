import { mount } from '../../shared/dom';
import { Home } from './Home';
import { initPlayer } from './player';
import { initTakeaway } from './takeaway';
import { initClips } from './clips';
import { initArcadeTabs } from './tabs';

// In the order the page's scripts ran when it was static HTML; news.js follows (deferred).
mount(<Home />);
initPlayer();
initTakeaway();
initClips();
initArcadeTabs();
