export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        cream: {
          50: '#F5F3F0',
          100: '#E8E2D4',
          200: '#DDD6C8',
        },
        green: {
          50: '#F0F4F1',
          600: '#436741', // primary
          700: '#2F4A2E', // hover
          800: '#243620', // pressed
        },
        rose: {
          50: '#FAF7F7',
          100: '#E1C1C6', // primary
          200: '#C9A4AA', // hover
          300: '#B08A90', // pressed
        },
        charcoal: '#1A1A1A',
      },
      fontFamily: {
        serif: ['Playfair Display', 'serif'],
        sans: ['system-ui', 'sans-serif'],
      },
      boxShadow: {
        retro: '4px 4px 0px rgba(26, 26, 26, 0.08)',
        'retro-green': '4px 4px 0px rgba(67, 103, 65, 0.12)',
        'retro-rose': '4px 4px 0px rgba(225, 193, 198, 0.12)',
        'retro-lg': '6px 6px 0px rgba(26, 26, 26, 0.1)',
      },
    },
  },
  plugins: [],
}
