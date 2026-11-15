// What's new: whether a commit subject is housekeeping (code tidy-ups, docs, fitting a round's changes together,
// retaken pictures) rather than something a visitor would notice. What's new folds these into "Behind the scenes",
// and an app's "Latest change" line skips them. Only the subject is read, so commit subjects keep the patterns below.
const QUIET = new RegExp([
  '^(refactor\\b|runtime:|site code:|media scripts:|shim:|round \\d+\\b)',   // tidy-ups and round fit-ups
  'readme\\b', 'check:', 'prompt\\.md', 'upgrade vite', "convert the site's pages",   // docs, tests, tooling
  'retake', 'share card', '(description|hint) (now )?mentions',   // pictures and descriptions catching up
].join('|'), 'i');

export const quiet = (subject: string) => QUIET.test(subject);

const squash = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '');
/** The one app a change is to, when its title is only that app's name ("SimpleAlgorithms: ..."), else null. */
export function titledApp(it: { title: string; apps: OldjavaCatalogApp[] }) {
  const a = it.apps.length === 1 ? it.apps[0] : null;
  return a && (squash(it.title) === squash(a.id) || squash(it.title) === squash(a.name)) ? a : null;
}
