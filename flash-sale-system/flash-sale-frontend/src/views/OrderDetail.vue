<template>
  <div v-if="order" class="order-detail">
    <div class="back-row">
      <span class="back" @click="$router.back()">←</span>
      <span class="head-title">订单详情</span>
    </div>
    <div class="detail-card">
      <div class="row-label">订单号</div>
      <div class="order-no">{{ order.orderNo }}</div>
      <div class="row-label">商品</div>
      <div class="order-name">{{ order.activityName || (order.activityId ? '秒杀商品' : '普通商品') }}</div>
      <div class="row-label">金额</div>
      <div class="order-price">¥{{ order.totalPrice }}</div>
      <div class="order-tag">
        <el-tag :type="['warning', 'success', 'info', 'danger'][order.status]">{{ ['待支付', '已支付', '已取消', '已退款'][order.status] }}</el-tag>
      </div>
      <div class="order-time">{{ (order.createTime || '').replace('T', ' ') }}</div>
    </div>
    <el-button class="back-btn" @click="$router.push('/orders')">返回订单列表</el-button>
  </div>
  <div v-else-if="notFound" class="empty-box">
    <el-empty description="订单不存在或无权查看">
      <el-button type="primary" @click="$router.push('/orders')">返回订单列表</el-button>
    </el-empty>
  </div>
  <div v-else class="empty-box loading">加载中...</div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder, getActivity } from '@/api'
const route = useRoute()
const order = ref(null)
const notFound = ref(false)
onMounted(async () => {
  try {
    const r = await getOrder(route.params.orderId)
    if (r.data.code === 200 && r.data.data) {
      order.value = r.data.data
      if (order.value.activityId) {
        try {
          const a = await getActivity(order.value.activityId)
          if (a.data.code === 200 && a.data.data) order.value.activityName = a.data.data.activityName
        } catch {}
      }
    } else {
      notFound.value = true
    }
  } catch {
    notFound.value = true
  }
})
</script>
<style scoped>
.order-detail {
  max-width: 600px;
  margin: 0 auto;
}

.back-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.back {
  font-size: 20px;
  cursor: pointer;
  color: var(--ink-2);
}

.back:hover {
  color: var(--brand);
}

.head-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}

.detail-card {
  background: #fff;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  padding: 24px;
  margin-bottom: 16px;
}

.row-label {
  font-size: 12px;
  color: var(--ink-3);
  margin-bottom: 4px;
}

.order-no {
  font-size: 14px;
  color: var(--ink-2);
  margin-bottom: 16px;
}

.order-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 16px;
}

.order-price {
  font-size: 28px;
  font-weight: 800;
  color: var(--brand);
  margin-bottom: 16px;
}

.order-tag {
  margin-bottom: 12px;
}

.order-time {
  font-size: 12px;
  color: var(--ink-3);
}

.back-btn {
  width: 100%;
  border-radius: 10px;
  height: 42px;
}

.empty-box {
  text-align: center;
  padding: 60px 0;
}

.loading {
  color: var(--ink-3);
}
</style>
