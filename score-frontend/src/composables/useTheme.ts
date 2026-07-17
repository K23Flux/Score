import { ref, watch, onMounted } from 'vue'

export function useTheme() {
  const isDark = ref(false)

  function setTheme(theme: 'light' | 'dark') {
    if (theme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
    localStorage.setItem('theme', theme)
    isDark.value = theme === 'dark'
  }

  function toggleTheme() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  onMounted(() => {
    const saved = localStorage.getItem('theme')
    if (saved) {
      setTheme(saved as 'light' | 'dark')
    } else {
      // No saved preference — detect system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      setTheme(prefersDark ? 'dark' : 'light')
    }
  })

  return { isDark, setTheme, toggleTheme }
}
