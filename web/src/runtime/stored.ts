// A visitor's choice kept in localStorage. Storage can be blocked (a private window, site data turned off): then
// stored() gives null, as if nothing was kept, and store() keeps nothing, so the choice lasts this page only.
export function stored(key: string) { try { return localStorage.getItem(key); } catch (e) { return null; } }
export function store(key: string, value: string) { try { localStorage.setItem(key, value); } catch (e) { /* this page only */ } }
/** An object kept as JSON, or {} when there is none, storage is blocked, or what is kept is not an object. */
export function storedJson(key: string): Record<string, any> {
  try { const v = JSON.parse(stored(key) || '{}'); return v && typeof v === 'object' ? v : {}; } catch (e) { return {}; }
}
