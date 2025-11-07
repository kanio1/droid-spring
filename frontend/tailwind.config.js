/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './app/**/*.{js,ts,vue,css}',
    './components/**/*.{js,ts,vue,css}',
    './layouts/**/*.{js,ts,vue,css}',
    './pages/**/*.{js,ts,vue,css}',
    './plugins/**/*.{js,ts,vue,css}',
    './nuxt.config.{js,ts}'
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: 'var(--color-primary)',
        'primary-hover': 'var(--color-primary-hover)',
        'primary-light': 'var(--color-primary-light)',
        secondary: 'var(--color-secondary)',
        'secondary-hover': 'var(--color-secondary-hover)',
        'secondary-light': 'var(--color-secondary-light)',
        accent: 'var(--color-accent)',
        'accent-hover': 'var(--color-accent-hover)',
        'accent-light': 'var(--color-accent-light)',
        warning: 'var(--color-warning)',
        'warning-hover': 'var(--color-warning-hover)',
        'warning-light': 'var(--color-warning-light)',
        success: 'var(--color-success)',
        'success-hover': 'var(--color-success-hover)',
        'success-light': 'var(--color-success-light)',
        danger: 'var(--color-danger)',
        'danger-hover': 'var(--color-danger-hover)',
        'danger-light': 'var(--color-danger-light)',
        surface: 'var(--color-surface)',
        'surface-alt': 'var(--color-surface-alt)',
        border: 'var(--color-border)',
        'text-primary': 'var(--color-text-primary)',
        'text-secondary': 'var(--color-text-secondary)',
        'text-muted': 'var(--color-text-muted)'
      },
      fontFamily: {
        sans: ['var(--font-family-base)', 'system-ui', 'sans-serif']
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem'
      },
      borderRadius: {
        'xl': 'var(--radius-xl)',
        '2xl': '1rem',
        '3xl': '1.5rem'
      },
      boxShadow: {
        'md': 'var(--shadow-md)',
        'lg': 'var(--shadow-lg)',
        'xl': 'var(--shadow-xl)'
      }
    }
  },
  plugins: [
    require('@headlessui/tailwindcss')
  ]
}
