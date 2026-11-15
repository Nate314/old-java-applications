/**
 * A script of src/runtime/lazy, added the first time it is wanted: the returned function gives the promise of the
 * API the script puts on window (api reads it). A load that fails is tried again on the next call.
 */
export function lazyScript<T>(load: () => Promise<unknown>, api: () => T | undefined, name: string): () => Promise<T> {
  let promise: Promise<T> | null = null;
  return function () {
    if (!promise) promise = load().then(function () { return api()!; }, function () { promise = null; throw new Error(name + ' did not load'); });
    return promise;
  };
}
