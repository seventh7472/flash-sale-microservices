<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
              <path d="M12 1.5 5 12h5l-2 10.5L19 9h-5l3.5-7.5L12 1.5z" fill="currentColor" />
            </svg>
          </span>
          <span class="brand-name">特价商城</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/cart" class="nav-link" active-class="active">
            购物车
            <span v-if="c.cnt" class="badge">{{ c.cnt > 99 ? '99+' : c.cnt }}</span>
          </router-link>
          <router-link v-if="a()" to="/orders" class="nav-link" active-class="active">订单</router-link>
          <router-link v-if="a() && s.isAdmin()" to="/admin" class="nav-link" active-class="active">管理</router-link>
          <router-link v-if="a() && s.isAdmin()" to="/admin/ai" class="nav-link" active-class="active">AI</router-link>
          <router-link v-if="a() && s.isAdmin()" to="/ai-cheat" class="nav-link" active-class="active">AI防作弊</router-link>
        </nav>

        <div class="right">
          <template v-if="!a()">
            <router-link to="/login" class="login-btn">登录</router-link>
          </template>
          <template v-else>
            <span class="user-name">{{ s.username || '用户' }}</span>
            <button class="logout-btn" @click="l">退出</button>
          </template>
        </div>
      </div>
    </header>

    <main class="page-container">
      <router-view />
    </main>

    <footer class="footer">
      智能电商秒杀系统 · SpringCloud + Vue + AI
    </footer>
  </div>
</template>

<script setup>
import { useCartStore } from '@/store/cart'
import { useAuthStore } from '@/store/auth'
const c = useCartStore()
const s = useAuthStore()
function a() { return !!s.userId }
function l() { s.logout(); window.location.href = '/login' }
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  background: #1a1412;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(26, 20, 18, 0.18);
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff7a00, #ff4d2e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(255, 77, 46, 0.4);
}

.brand-name {
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

.nav-link {
  position: relative;
  color: #c9c1bd;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 8px;
  white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-link.active {
  color: #fff;
  font-weight: 600;
}

.nav-link.active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 2px;
  height: 2px;
  background: #ff4d2e;
  border-radius: 1px;
}

.badge {
  position: absolute;
  top: 2px;
  right: 0;
  background: #ff4d2e;
  color: #fff;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 700;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-name {
  color: #c9c1bd;
  font-size: 13px;
}

.login-btn {
  background: #ff4d2e;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 18px;
  border-radius: 8px;
  transition: background 0.15s;
}

.login-btn:hover {
  background: #e63b1f;
}

.logout-btn {
  background: transparent;
  border: 1px solid #4a403c;
  color: #c9c1bd;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.logout-btn:hover {
  border-color: #ff4d2e;
  color: #ff4d2e;
}

.page-container {
  flex: 1;
}

.footer {
  text-align: center;
  color: #978b86;
  font-size: 12px;
  padding: 20px 16px;
  border-top: 1px solid var(--line);
  background: #fff;
}

@media (max-width: 768px) {
  .topbar-inner {
    gap: 12px;
    height: 54px;
  }
  .brand-name {
    font-size: 16px;
  }
  .nav-link {
    padding: 8px 10px;
    font-size: 13px;
  }
  .user-name {
    display: none;
  }
}
</style>
