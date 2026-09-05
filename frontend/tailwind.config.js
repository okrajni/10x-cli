/**
 * Global visual system.
 *
 * The palette is intentionally locked to two colors:
 *   canvas #344c36 — application background / text on accent surfaces
 *   accent #D29C9A — borders, text, headings, links, button fills
 *
 * `theme.colors` is REPLACED (not extended) on purpose: Tailwind's default
 * gray / red / blue / green scales are unavailable, so an off-palette class
 * cannot silently slip into the UI. Depth comes from alpha of the accent
 * over the canvas (e.g. bg-accent/10), never from new hues.
 */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    colors: {
      transparent: 'transparent',
      current: 'currentColor',
      inherit: 'inherit',
      canvas: '#344c36',
      accent: '#D29C9A',
    },
    extend: {
      fontFamily: {
        /**
         * `brand` is reserved for the application name only.
         * Rammetto One is self-hosted from public/fonts/ and committed to the
         * repo — SIL Open Font License, free for commercial use.
         * See frontend/public/fonts/README.md.
         */
        brand: ['Rammetto One', 'Playfair Display', 'Georgia', 'serif'],
        serif: ['Playfair Display', 'Georgia', 'serif'],
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      borderRadius: {
        pill: '9999px',
        card: '1.75rem',
        field: '1.25rem',
      },
      borderWidth: {
        hairline: '1px',
      },
      letterSpacing: {
        label: '0.14em',
      },
      maxWidth: {
        content: '68rem',
      },
      transitionDuration: {
        DEFAULT: '200ms',
      },
    },
  },
  plugins: [],
}
