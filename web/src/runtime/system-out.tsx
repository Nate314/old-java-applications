import { useLayoutEffect, useRef, useState } from 'react';
import { openLayer } from './ui/layer';

let outLine: string;
/** Adds a chunk of System.out to the page's console panel, once it shows one (<body data-console>). */
let print: ((chunk: string) => void) | null;

/** The console panel at the bottom of the page, for System.out; returns its element. */
export function showConsole() {
  var el!: HTMLElement;
  openLayer(<Console onMount={(e, p) => { el = e; print = p; }} />);
  return el;
}

/** What the app printed, each chunk as it came, kept scrolled to the end. */
function Console({ onMount }: { onMount: (el: HTMLElement, print: (chunk: string) => void) => void }) {
  const pre = useRef<HTMLPreElement>(null);
  const [chunks, setChunks] = useState<string[]>([]);
  useLayoutEffect(() => { onMount(pre.current!, chunk => setChunks(l => l.concat([chunk]))); }, []);
  useLayoutEffect(() => { pre.current!.scrollTop = pre.current!.scrollHeight; }, [chunks]);
  return <pre className="shim-console" ref={pre}>{chunks}</pre>;
}

export function init() {
  // ---- System.out ----
  // TeaVM calls $rt_putStdoutCustom with raw chunks when it is defined before classes.js loads.
  outLine = '';
  print = null;
  window.$rt_putStdoutCustom = function (chunk) {
    if (print) print(chunk);
    var lines = (outLine + chunk).split('\n');
    outLine = lines.pop()!;
    lines.forEach(function (line) { console.info(line); });
  };
}
