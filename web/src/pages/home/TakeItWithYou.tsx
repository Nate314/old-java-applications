// The Take it with you tab of Your arcade: Install app, Save for offline and the cartridge case, drawn from H.take,
// which takeaway.ts keeps. (Not Takeaway.tsx: on Windows it would clash with takeaway.ts.)
import './take-it-with-you.css';
import { APPS, webp } from '../../shared/catalog';
import { H } from './model';
import * as P from './player';
import { countSaved, installClick, saveClick, saveOne } from './takeaway';

export function TakeItWithYou({ off }: { off: string }) {
  const t = H.take;
  return (
    <section className={'takeaway' + off} id="takeaway" role="tabpanel" aria-labelledby="tab-takeaway" hidden={!t.shown}>
      <div className="takeaway-head">
        <img className="app-icon" src="icons/icon.svg" alt="" width="76" height="76" />
        <div className="text">
          <h2 id="takeaway-title">Take the arcade with you</h2>
          <p id="takeaway-text" aria-live="polite">{t.text}</p>
          <div className="meter" id="takeaway-meter" hidden={!t.meterShown}><span style={t.meterW !== null ? { width: t.meterW + '%' } : undefined} /></div>
        </div>
        <div className="actions">
          <button className="btn primary" type="button" id="takeaway-install" hidden={!t.install} onClick={installClick}>Install app</button>
          {' '}
          <button className="btn" type="button" id="takeaway-save" disabled={t.saving} onClick={saveClick}>{t.saveText}</button>
        </div>
      </div>
      <Case />
      <ul className="perks"><li>Its own window and icon</li><li>Saved apps play with no connection</li><li>No account, no ads</li></ul>
      <p className="note" id="takeaway-note" hidden={!t.note}>To install, use Install app or Add to Home Screen in your browser's menu.</p>
    </section>
  );
}

/**
 * The cartridge case: every app as a cartridge, lit once it is saved on this device (it then plays here, offline
 * too), and a button to save that app alone while it is not. Hidden until the cache has been checked.
 */
function Case() {
  const t = H.take;
  if (!t.checked) return null;
  const count = countSaved();
  return (
    <div className="case">
      <p className="case-head">
        <strong>{count + ' of ' + APPS.length + ' saved'}</strong>
        {t.used && count ? ' · ' + (t.used / 1048576).toFixed(1) + ' MB on this device' : ''}
        <span>{t.offline ? ' · Lit ones play now' : ' · Tap one to save just that app'}</span>
      </p>
      <ul className="carts" aria-label="Apps on this device">
        {APPS.map(a => {
          const ok = !!t.saved[a.id], busy = !!t.queued[a.id];
          const inner = (
            <>
              <img src={webp(a.shot, true)} alt="" loading="lazy" width="120" height="90" />
              <span className="cart-name">{a.name}</span>
              <span className="cart-state">{busy ? 'Saving…' : ok ? 'Saved ✓' : t.offline ? 'Needs a connection' : 'Save'}</span>
            </>
          );
          return (
            <li key={a.id} className={'cart' + (ok && !busy ? ' lit' : '') + (busy ? ' busy' : '')}>
              {ok && !busy
                ? <a href={a.id + '/index.html'} title={'Play ' + a.name + ' (saved on this device)'} onClick={e => P.cardClick(e, a)}>{inner}</a>
                : <button type="button" disabled={t.saving || t.offline} title={'Save ' + a.name + ' to play with no connection'}
                    aria-label={(busy ? 'Saving ' : 'Save ') + a.name} onClick={() => saveOne(a.id)}>{inner}</button>}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
