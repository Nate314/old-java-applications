// The pages' own copy of the runtime's storedJson (src/runtime/stored.ts): importing runtime code into a page
// bundle would add a chunk. Used by the home page (saves, tour) and the lost page (last played).

/** The object kept under this localStorage key, or {} when there is none, it is not an object, or storage is blocked. */
export function readJson(key: string): Record<string, any> {
  try { const v = JSON.parse(localStorage.getItem(key) || '{}'); return v && typeof v === 'object' ? v : {}; } catch (e) { return {}; }
}
