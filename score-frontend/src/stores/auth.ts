import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('satoken') || '')
  const userName = ref<string>(localStorage.getItem('userName') || '')
  const className = ref<string>(localStorage.getItem('className') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('satoken', newToken)
  }

  function setUserInfo(name: string, classN: string) {
    userName.value = name
    className.value = classN
    localStorage.setItem('userName', name)
    localStorage.setItem('className', classN)
  }

  function logout() {
    token.value = ''
    userName.value = ''
    className.value = ''
    localStorage.removeItem('satoken')
    localStorage.removeItem('userName')
    localStorage.removeItem('className')
  }

  return {
    token, userName, className, isLoggedIn,
    setToken, setUserInfo, logout
  }
})
