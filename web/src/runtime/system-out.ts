let outLine: string;
export let outPanel: HTMLElement | null;

export function setOutPanel(v: HTMLElement | null) { outPanel = v; }

export function init() {
  // ---- System.out ----
  // TeaVM calls $rt_putStdoutCustom with raw chunks when it is defined before classes.js loads.
  outLine = '';
 outPanel = null;
  window.$rt_putStdoutCustom = function (chunk) {
    if (outPanel) {
      outPanel.appendChild(document.createTextNode(chunk));
      outPanel.scrollTop = outPanel.scrollHeight;
    }
    var lines = (outLine + chunk).split('\n');
    outLine = lines.pop()!;
    lines.forEach(function (line) { console.info(line); });
  };
}
