<template>
  <div>
    <div class="cart-head">
      <h2 class="title">购物车</h2>
      <div class="actions">
        <el-button v-if="cart.items.length" size="small" @click="cart.clr()">清空</el-button>
        <el-button v-if="cart.items.length" type="primary" size="small" :loading="checking" @click="checkout">去结算（¥{{ cart.tot }}）</el-button>
      </div>
    </div>
    <el-empty v-if="!cart.items.length" description="购物车是空的，去逛逛吧" />
    <div
      v-for="item in cart.items"
      :key="item.t + '-' + item.id"
      class="cart-item"
    >
      <div class="item-icon">{{ ic(item.n) }}</div>
      <div class="item-info">
        <div class="item-name">{{ item.n }}</div>
        <div class="item-price">¥{{ item.p }}</div>
        <el-tag v-if="item.t === 'flash'" type="danger" size="small" effect="dark" class="item-tag">限时秒杀</el-tag>
        <el-tag v-else type="primary" size="small" effect="plain" class="item-tag">普通商品</el-tag>
      </div>
      <div class="item-right">
        <span class="qty">×{{ item.q }}</span>
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
  if (n.includes('Phone') || n.includes('Smartphone') || n.includes('iPhone')) return '📱'
  if (n.includes('Laptop') || n.includes('MacBook') || n.includes('iPad') || n.includes('ThinkPad')) return '💻'
  if (n.includes('Headphones') || n.includes('Earbuds') || n.includes('AirPods') || n.includes('Speaker')) return '🎧'
  if (n.includes('Watch') || n.includes('Fitness')) return '⌚'
  if (n.includes('Camera') || n.includes('EOS') || n.includes('GoPro')) return '📷'
  if (n.includes('Dyson') || n.includes('Vacuum') || n.includes('Roomba')) return '🏠'
  if (n.includes('PlayStation') || n.includes('Switch') || n.includes('Xbox')) return '🎮'
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
<style scoped>
.cart-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 20px;
  margin: 0;
  color: var(--ink);
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 8px;
}

.cart-item {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: var(--r-md);
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid var(--line);
  transition: box-shadow 0.2s, transform 0.2s;
}

.cart-item:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.item-icon {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: #f4efeb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: var(--ink);
  margin-bottom: 6px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-price {
  font-size: 18px;
  font-weight: 800;
  color: var(--brand);
  margin-bottom: 6px;
}

.item-tag {
  margin-top: 2px;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qty {
  font-size: 13px;
  color: var(--ink-3);
}
</style>
