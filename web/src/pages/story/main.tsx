import { hydrateRoot } from 'react-dom/client';
import { byId, mount } from '../../shared/dom';
import { StoryTop } from './panels';
import { Story } from './Story';
import { registerAfterLoad } from '../../sw/register';

// story.html comes with its top bar and hero (src/routes.tsx), painted before this runs: the bar is taken over as it
// is, the hero stays, and the chapters go in after it.
hydrateRoot(byId('story-top'), <StoryTop />);
mount(<Story />, 'story-rest');
registerAfterLoad();   // a visit that starts here plays offline later too (no boot.js on this page)
