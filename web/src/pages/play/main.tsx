import { mount } from '../../shared/dom';
import { registerAfterLoad } from '../../sw/register';
import { Play } from './Play';

mount(<Play />);
// Opened on its own, play.html plays offline later too. Framed on another site it waits: the app page registers the
// worker once Play is pressed, so an embed that is never played does not download the whole build.
if (window.top === window) registerAfterLoad();
