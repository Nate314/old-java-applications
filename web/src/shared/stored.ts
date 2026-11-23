// The pages' own copy of the runtime's stored, store and storedJson (src/runtime/stored.ts): importing runtime code
// into a page bundle would add a chunk. Used by the home page (saves, tour, tabs, What's new) and the lost page.

/** The string kept under this localStorage key, or null when there is none or storage is blocked. */
export function stored(key: string) { try { return localStorage.getItem(key); } catch (e) { return null; } }
/** Keeps a string under this key; with storage blocked nothing is kept, so it lasts this page only. */
export function store(key: string, value: string) { try { localStorage.setItem(key, value); } catch (e) { /* this page only */ } }
/** The object kept under this localStorage key, or {} when there is none, it is not an object, or storage is blocked. */
export function readJson(key: string): Record<string, any> {
  try { const v = JSON.parse(stored(key) || '{}'); return v && typeof v === 'object' ? v : {}; } catch (e) { return {}; }
}
