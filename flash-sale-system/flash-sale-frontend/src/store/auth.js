import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin } from '@/api'

function parseJwtPayload(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(decodeURIComponent(escape(atob(payload))))
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  async function login(usernameVal, password) {
    const r = await apiLogin(usernameVal, password)
    if (r.data.code !== 200) {
      throw new Error(r.data.message || '登录失败')
    }
    const jwt = r.data.data
    const claims = parseJwtPayload(jwt)
    token.value = jwt
    userId.value = claims.uid != null ? String(claims.uid)
      : (claims.userId != null ? String(claims.userId) : usernameVal)
    username.value = claims.username || usernameVal
    role.value = claims.role || 'USER'
    localStorage.setItem('token', token.value)
    localStorage.setItem('userId', userId.value)
    localStorage.setItem('username', username.value)
    localStorage.setItem('role', role.value)
    return true
  }

  function registerUser(usernameVal) {
    const id = String(Date.now()).slice(-6)
    userId.value = id; username.value = usernameVal
    role.value = 'USER'
    localStorage.setItem('userId', id); localStorage.setItem('username', usernameVal)
    localStorage.setItem('role', role.value)
    return true
  }

  function logout() {
    token.value = ''; userId.value = ''; username.value = ''; role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  function isLoggedIn() { return !!token.value }
  function isAdmin() { return role.value === 'ADMIN' }
  return { token, userId, username, role, login, registerUser, logout, isLoggedIn, isAdmin }
})