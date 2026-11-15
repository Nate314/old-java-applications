// Share the story, or the chapter you are reading: the phone's share sheet where there is one, else the link is
// copied. The link preview it unfurls into is cards/story.jpg (web/cards.ps1, the share tags in src/routes.tsx).
import { useEffect, useState } from 'react';

const TITLE = 'The story: Old Java Applications';

/** hash: the chapter to link to ('#recovery'), or none for the whole story; name: that chapter's name. */
export function ShareStory({ hash, name, label, className }: { hash?: string; name?: string; label: string; className: string }) {
  const [said, setSaid] = useState('');
  useEffect(() => {
    if (!said) return;
    const t = setTimeout(() => setSaid(''), 2500);
    return () => clearTimeout(t);
  }, [said]);

  function share() {
    const url = location.origin + location.pathname + (hash || '');
    const title = name ? name + ' | ' + TITLE : TITLE;
    if (navigator.share) {
      navigator.share({ title, url }).catch(() => {});   // closing the sheet is no error
      return;
    }
    (navigator.clipboard ? navigator.clipboard.writeText(url) : Promise.reject()).then(
      () => setSaid(name ? 'Link to “' + name + '” copied' : 'Link copied'),
      () => setSaid('Copy this link: ' + url));
  }

  return (
    <>
      <button type="button" className={className} onClick={share}>{'↗ ' + label}</button>
      <span className="share-said" role="status">{said}</span>
    </>
  );
}
