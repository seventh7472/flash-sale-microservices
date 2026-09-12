import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/errorMessages'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})
api.interceptors.request.use(c => {
  const token = localStorage.getItem('token')
  if (token) c.headers['Authorization'] = `Bearer ${token}`
  const uid = localStorage.getItem('userId')
  if (uid) c.headers['X-User-Id'] = uid
  return c
})

api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status
    const businessCode = error.response?.data?.code
    const code = typeof businessCode === 'number' ? businessCode : status
    if (status === 401) {
      // token 缺失/过期：清理登录态并跳转登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login'
    }
    ElMessage.error(getErrorMessage(code))
    return Promise.reject(error)
  }
)

export const login = (u, p) => api.post('/auth/login', { username: u, password: p })
export const register = (u, p) => api.post('/auth/register', { username: u, password: p })

export const listProducts = (p, s) => api.get('/product/list', { params: { page: p, size: s } })
export const getProduct = id => api.get(`/product/detail/${id}`)
export const createProduct = d => api.post('/product/create', d)
export const updateProduct = d => api.put('/product/update', d)
export const deleteProduct = id => api.delete(`/product/delete/${id}`)

export const listActivities = () => api.get('/flash-sale/list')
export const listAllActivities = () => api.get('/flash-sale/admin/list')
export const getActivity = id => api.get(`/flash-sale/detail/${id}`)
export const createActivity = (d, autoWarmUp) => api.post('/flash-sale/create', d, { params: { autoWarmUp: !!autoWarmUp } })
export const seckill = id => api.post(`/flash-sale/seckill/${id}`, null, {
  params: { idempotencyToken: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}` }
})

export const getInventoryStock = id => api.get(`/inventory/stock/${id}`)
export const warmUpStock = (id, s) => api.post('/inventory/warm-up', null, { params: { activityId: id, stock: s } })

export const createNormalOrder = (productId, quantity = 1) => api.post('/order/normal/create', null, { params: { productId, quantity } })
export const listOrders = () => api.get('/order/list')
export const getOrder = no => api.get(`/order/detail/${no}`)
export const payOrder = no => api.post(`/order/pay/${no}`)
export const cancelOrder = no => api.post(`/order/cancel/${no}`)

export const predictTraffic = aid => api.post('/ai/predict-traffic', { activityId: aid })

export const listCheatUsers = () => api.get('/ai/cheat-users')
export const listCheatBlacklist = () => api.get('/ai/cheat-blacklist')
export const banCheatUser = userId => api.post(`/ai/cheat-ban/${userId}`)
