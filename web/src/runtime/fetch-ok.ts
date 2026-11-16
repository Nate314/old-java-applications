/** fetch that also fails on an HTTP error status, with "404 Not Found" as the message. */
export function fetchOk(url: string, init?: RequestInit): Promise<Response> {
  return fetch(url, init).then(function (r) {
    if (!r.ok) throw new Error(r.status + ' ' + r.statusText);
    return r;
  });
}
