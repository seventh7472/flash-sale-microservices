<template>
  <div style="min-height:100vh;background:#f5f7fa">
    <div style="display:flex;align-items:center;gap:14px;padding:10px 20px;background:linear-gradient(135deg,#1a73e8,#0d47a1)">
      <router-link to="/" style="color:#fff;font-size:18px;font-weight:700;text-decoration:none">特价商城</router-link>
      <span style="flex:1"></span>
      <router-link to="/" style="color:#fff;font-size:13px;text-decoration:none">首页</router-link>
      <router-link to="/cart" style="color:#fff;font-size:13px;text-decoration:none;position:relative">购物车<span v-if="c.cnt" style="position:absolute;top:-6px;right:-14px;background:#f53f2d;color:#fff;font-size:10px;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 3px;font-weight:700">{{ c.cnt>99?"99+":c.cnt }}</span></router-link>
      <router-link v-if="a()" to="/orders" style="color:#fff;font-size:13px;text-decoration:none">订单</router-link>
      <router-link v-if="a() && s.isAdmin()" to="/admin" style="color:#fff;font-size:13px;text-decoration:none">管理</router-link>
      <router-link v-if="a() && s.isAdmin()" to="/admin/ai" style="color:#fff;font-size:13px;text-decoration:none">AI</router-link>
      <router-link v-if="a() && s.isAdmin()" to="/ai-cheat" style="color:#fff;font-size:13px;text-decoration:none">AI防作弊</router-link>
      <router-link v-if="!a()" to="/login" style="color:#fff;font-size:13px;text-decoration:none">登录</router-link>
      <span v-else style="color:#fff;font-size:13px;cursor:pointer" @click="l">退出</span>
    </div>
    <div style="max-width:1200px;margin:0 auto;padding:16px"><router-view /></div>
  </div>
</template>
<script setup>
import { useCartStore } from '@/store/cart'
import { useAuthStore } from '@/store/auth'
const c = useCartStore()
const s = useAuthStore()
function a() { return !!s.userId }
function l() { s.logout(); window.location.href='/login' }
</script>
<style>body{margin:0;font-family:-apple-system,sans-serif}</style>
