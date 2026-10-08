/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f4ff',
          100: '#e0ecff',
          200: '#c0d4ff',
          300: '#a0bfff',
          400: '#80a6ff',
          500: '#0f3d7a',  // Azul profundo principal
          600: '#0c3062',
          700: '#0a244d',
          800: '#081a3a',
          900: '#061026',
        },
        accent: {
          50: '#fdfaf0',
          100: '#f9f4e1',
          200: '#f0e9c6',
          300: '#e5d9a8',
          400: '#d9c98a',
          500: '#c5a065',  // Dourado principal
          600: '#b18d55',
          700: '#9d7a45',
          800: '#896735',
          900: '#755425',
        },
        gold: {
          DEFAULT: '#c5a065',
          50: '#fdfaf0',
          100: '#f9f4e1',
          200: '#f0e9c6',
          300: '#e5d9a8',
          400: '#d9c98a',
          500: '#c5a065',
          600: '#b18d55',
          700: '#9d7a45',
          800: '#896735',
          900: '#755425',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-in': 'slideIn 0.3s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(-100%)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
      },
    },
  },
  plugins: [],
}