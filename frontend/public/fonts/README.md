# Wordmark font — Rammetto One

The application name ("done yet?") is set in **Rammetto One**, a heavy geometric
display face based on Basuto, a 1926 Stephenson Blake uppercase display type.

- Specimen: https://fonts.google.com/specimen/Rammetto+One
- Source: https://github.com/googlefonts/RammettoFont
- License: **SIL Open Font License 1.1** — see `OFL.txt` in this directory.

## License, in short

Free. Free for commercial use, free to embed, free to self-host, free to
redistribute — which is why the font file is **committed to this repo** rather
than downloaded per developer. No purchase, no CI secret, no per-machine setup.

Two OFL conditions worth knowing:

- `OFL.txt` must travel with the font files. It is in this directory — keep it
  there.
- "Rammetto" is a Reserved Font Name: if you ever modify the font (subset it,
  rename it, patch glyphs), the derivative must not be called Rammetto.

Subsetting for size is a modification, so a subset build needs a different
family name. Given the wordmark is three words, that trade may be worth it —
but only if page weight ever becomes a real problem.

## Adding the file (one-time, if it is missing)

1. Download from https://fonts.google.com/specimen/Rammetto+One ("Get font" →
   "Download all"), or take `RammettoOne-Regular.ttf` from
   https://github.com/google/fonts/tree/main/ofl/rammettoone
2. Convert to webfonts — e.g. https://transfonter.org with WOFF2 + WOFF enabled.
3. Place them here with exactly these names:

   ```
   frontend/public/fonts/rammetto-one.woff2
   frontend/public/fonts/rammetto-one.woff
   ```

4. Commit them. Vite serves `public/` at the site root, so the `@font-face`
   rule in `src/index.css` picks them up with no code change.

## Why self-hosted rather than the Google Fonts CDN

One less third-party connection on first paint, no external dependency for the
wordmark, and no requests leaving for fonts.gstatic.com — which also keeps the
app simpler to reason about under GDPR. The CDN alternative is a one-line
change if you would rather not carry the file: add `Rammetto+One` to the
existing `@import` in `src/index.css` and delete the `@font-face` block.

## Until the file is present

`font-brand` (see `tailwind.config.js`) falls back to Playfair Display, which is
already loaded for headings. The wordmark still renders as a display face —
just not the intended one.

## Scope

Only the wordmark uses this font. Body copy stays on Inter, other headings on
Playfair Display; Rammetto One is far too heavy for either.

## The static backend page

`src/main/resources/static/index.html` (the Spring landing page) declares its
own `@font-face` for the same font and expects the file at
`/fonts/rammetto-one.woff2`, i.e. `src/main/resources/static/fonts/rammetto-one.woff2`.
Copy the same file (and `OFL.txt`) there if you want the wordmark on that page.
