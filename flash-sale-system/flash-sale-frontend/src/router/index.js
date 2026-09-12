import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/flash/:id', component: () => import('@/views/FlashSaleDetail.vue') },
  { path: '/product/:id', component: () => import('@/views/ProductDetail.vue') },
  { path: '/cart', component: () => import('@/views/Cart.vue') },
  { path: '/orders', component: () => import('@/views/Orders.vue'), meta: { auth: true } },
  { path: '/order/:orderId', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue') },
  { path: '/admin', component: () => import('@/views/Admin.vue'), meta: { auth: true, requiresAdmin: true } },
  { path: '/admin/ai', component: () => import('@/views/AiDashboard.vue'), meta: { auth: true, requiresAdmin: true } },
  { path: '/ai-cheat', name: 'AiCheatDetect', component: () => import('@/views/AiCheatDetect.vue'), meta: { requiresAdmin: true } },
]
const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, _, next) => {
  if (to.meta.auth && !localStorage.getItem('userId')) return next('/login')
  if (to.meta.requiresAdmin && localStorage.getItem('role') !== 'ADMIN') {
    ElMessage.warning('无权限访问')
    return next('/')
  }
  next()
})
export default router