<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="font-size:18px;margin:0">购物车</h2>
      <div style="display:flex;gap:8px">
        <el-button v-if="cart.items.length" size="small" :loading="checking" type="danger" @click="checkout">去结算（¥{{ cart.tot }}）</el-button>
        <el-button v-if="cart.items.length" size="small" @click="cart.clr()">清空</el-button>
      </div>
    </div>
    <el-empty v-if="!cart.items.length" description="暂无数据" />
    <div v-for="item in cart.items" :key="item.t + '-' + item.id" style="display:flex;gap:12px;background:#fff;border-radius:10px;padding:12px;margin-bottom:10px;box-shadow:0 1px 4px rgba(0,0,0,0.04)">
      <div style="width:60px;height:60px;border-radius:8px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;font-size:28px;flex-shrink:0">{{ ic(item.n) }}</div>
      <div style="flex:1;min-width:0">
        <div style="font-size:13px;color:#333;margin-bottom:4px;display:-webkit-box;-webkit-line-clamp:2;overflow:hidden">{{ item.n }}</div>
        <div style="font-size:16px;font-weight:700;color:#f53f2d">¥{{ item.p }}</div>
        <el-tag v-if="item.t === 'flash'" type="danger" size="small" style="margin-top:4px">限时秒杀</el-tag>
        <el-tag v-else type="primary" size="small" style="margin-top:4px">普通商品</el-tag>
      </div>
      <div style="display:flex;flex-direction:column;align-items:center;gap:4px">
        <span style="font-size:12px;color:#999">x{{ item.q }}</span>
        <el-button size="small" type="danger" plain @click="cart.rm(item.id)">删除</el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/store/cart'
import { createNormalOrder, seckill } from '@/api'
const cart = useCartStore()
const router = useRouter()
const checking = ref(false)
function ic(n) {
  if (n.includes('Phone')||n.includes('Smartphone')||n.includes('iPhone')) return '📱'
  if (n.includes('Laptop')||n.includes('MacBook')||n.includes('iPad')||n.includes('ThinkPad')) return '💻'
  if (n.includes('Headphones')||n.includes('Earbuds')||n.includes('AirPods')||n.includes('Speaker')) return '🎧'
  if (n.includes('Watch')||n.includes('Fitness')) return '⌚'
  if (n.includes('Camera')||n.includes('EOS')||n.includes('GoPro')) return '📷'
  if (n.includes('Dyson')||n.includes('Vacuum')||n.includes('Roomba')) return '🏠'
  if (n.includes('PlayStation')||n.includes('Switch')||n.includes('Xbox')) return '🎮'
  return '📦'
}
async function checkout() {
  if (!localStorage.getItem('userId')) {
    ElMessage.warning('请先登录'); router.push('/login'); return
  }
  if (!cart.items.length || checking.value) return
  checking.value = true
  const snapshot = [...cart.items]
  const failed = []
  let ok = 0
  try {
    for (const item of snapshot) {
      try {
        let r
        if (item.t === 'flash') {
          r = await seckill(item.id)
        } else {
          r = await createNormalOrder(item.prod || item.id, item.q)
        }
        if (r.data.code !== 200) throw new Error(r.data.message || '下单失败')
        cart.rm(item.id)
        ok++
      } catch (e) {
        failed.push(item.n + '：' + (e.message || '下单失败'))
      }
    }
  } finally {
    checking.value = false
  }
  if (ok > 0) ElMessage.success('下单成功 ' + ok + ' 件，请到“我的订单”支付')
  if (failed.length) ElMessage.error('部分商品下单失败：' + failed.join('；'))
  if (ok > 0) router.push('/orders')
}
</script>