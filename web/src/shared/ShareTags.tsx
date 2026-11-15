// The site's name, and the link preview tags of a page with a share card (a 1200x630 JPEG that web/cards.ps1
// renders): Open Graph, which chat apps and social sites read, and Twitter's large card. Keep property before
// content: built with -Dsite.url, web/apps/pom.xml makes og:url and og:image absolute by matching that order.
export const SITE = 'Old Java Applications';

export function ShareTags({ type = 'website', title, description, url, image, alt }:
  { type?: string; title: string; description: string; url: string; image: string; alt: string }) {
  return (
    <>
      <meta property="og:type" content={type} />
      <meta property="og:site_name" content={SITE} />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />
      <meta property="og:url" content={url} />
      <meta property="og:image" content={image} />
      <meta property="og:image:width" content="1200" />
      <meta property="og:image:height" content="630" />
      <meta property="og:image:alt" content={alt} />
      <meta name="twitter:card" content="summary_large_image" />
    </>
  );
}
