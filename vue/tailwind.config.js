/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 暖调编辑/杂志风格色彩系统
        'novel': {
          'bg': '#faf7f2',
          'sidebar': '#f3efe8',
          'card': '#ffffff',
          'text': '#1a1815',
          'text-secondary': '#6b6560',
          'text-muted': '#9c9690',
          'accent': '#d97706',
          'accent-glow': 'rgba(217,119,6,0.15)',
          'teal': '#0e7490',
          'rose': '#be123c',
          'emerald': '#0d9488',
          'purple': '#7c3aed',
          'border': '#e8e3dc',
          'border-hover': '#d4cec6',
        }
      },
      fontFamily: {
        'display': ['Playfair Display', 'Noto Serif SC', 'serif'],
        'body': ['Crimson Pro', 'Noto Serif SC', 'serif'],
        'mono': ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        'sm': '6px',
        'DEFAULT': '10px',
        'lg': '16px',
        'xl': '20px',
      },
      boxShadow: {
        'sm': '0 1px 3px rgba(26,24,21,0.04)',
        'DEFAULT': '0 4px 16px rgba(26,24,21,0.06)',
        'lg': '0 12px 40px rgba(26,24,21,0.08)',
      },
      spacing: {
        'sidebar': '260px',
        'header': '56px',
      }
    },
  },
  plugins: [],
}
