// A Then page's way on to today's version (then.html): a postcard of today's picture on its title card, opening the
// two side by side (compare.html). Both the live card (title-screen.tsx) and the page's early card
// (src/pages/app/AppPage.tsx) draw it, so they match row for row. No imports: the build reads it in Node.

/** pic: today's 240 px picture; href: compare.html?app=<id>, relative to the page. */
export function NowPostcard({ pic, href }: { pic: string; href: string }) {
  return (
    <a className="shim-title-now" href={href}>
      <span className="shim-title-now-pic"><img alt="" src={pic} loading="lazy" decoding="async" /></span>
      <span className="shim-title-now-text">
        <strong>See what it became</strong>
        <span>Today’s version beside this one, with every change in between</span>
      </span>
    </a>
  );
}
