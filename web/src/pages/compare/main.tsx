import { mount } from '../../shared/dom';
import { Compare } from './Compare';
import './compare.css';

// The top bar and intro are written into compare.html (Intro.tsx); the rest goes in after them.
mount(<Compare />, 'compare-rest');
