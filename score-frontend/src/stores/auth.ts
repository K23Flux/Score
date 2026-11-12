import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('satoken') || '')
  const userName = ref<string>(localStorage.getItem('userName') || '')
  const className = ref<string>(localStorage.getItem('className') || '')
  const role = ref<string>(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('satoken', newToken)
  }

  function setUserInfo(name: string, classN: string, newRole = 'teacher') {
    userName.value = name
    className.value = classN
    role.value = newRole
    localStorage.setItem('userName', name)
    localStorage.setItem('className', classN)
    localStorage.setItem('role', newRole)
  }

  function logout() {
    token.value = ''
    userName.value = ''
    className.value = ''
    role.value = ''
    localStorage.removeItem('satoken')
    localStorage.removeItem('userName')
    localStorage.removeItem('className')
    localStorage.removeItem('role')
  }

  return {
    token, userName, className, role, isLoggedIn, isAdmin,
    setToken, setUserInfo, logout
  }
})
