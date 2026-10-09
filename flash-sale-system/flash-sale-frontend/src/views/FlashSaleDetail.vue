<template>
  <div>
    <div v-if="activity" class="flash-detail">
      <el-row :gutter="28">
        <el-col :xs="24" :sm="10">
          <el-image :src="product?.image" fit="cover" class="product-image" lazy>
            <template #error>
              <div class="image-placeholder">{{ productName.slice(0, 1) }}</div>
            </template>
          </el-image>
        </el-col>
        <el-col :xs="24" :sm="14">
          <div class="detail-info">
            <div class="flag-row">
              <span class="flag">限时秒杀</span>
            </div>
            <h1 class="product-name">{{ productName }}</h1>

            <div class="price-panel">
              <div class="price-main">
                <span class="price-sym">¥</span>
                <span class="flash-price">{{ activity.flashPrice }}</span>
                <span v-if="product?.originalPrice" class="original-price">¥{{ product.originalPrice }}</span>
              </div>
            </div>

            <div class="countdown-row">
              <span class="countdown-label">{{ countdownLabel }}</span>
              <template v-if="!isLongRunning">
                <span class="time-block" :class="{ urgent: isUrgent }">{{ hours }}</span>
                <span class="colon">:</span>
                <span class="time-block" :class="{ urgent: isUrgent }">{{ minutes }}</span>
                <span class="colon">:</span>
                <span class="time-block" :class="{ urgent: isUrgent }">{{ seconds }}</span>
              </template>
              <span v-else class="running-badge">活动进行中</span>
            </div>

            <div class="stock-row">
              <el-progress :percentage="soldPercent" :stroke-width="14" :color="progressColor" />
              <p class="stock-text">
                已抢 <b>{{ soldCount }}</b> / 共 {{ activity.totalStock }}，剩余 <b>{{ remainingText }}</b>
              </p>
            </div>

            <div class="action-row">
              <el-button type="primary" size="large" class="seckill-btn" :loading="seckillLoading" :disabled="buttonDisabled" @click="handleSeckill">
                {{ buttonText }}
              </el-button>
              <el-button size="large" class="cart-btn" @click="addToCart">加入购物车</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div v-else-if="loadError" class="state-box">
      <el-empty description="商品加载失败，请返回重试">
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
      </el-empty>
    </div>
    <div v-else class="state-box">
      <el-empty description="商品加载中..." :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity, getProduct, getInventoryStock, seckill } from '../api/index'
import { useAuthStore } from '../store/auth'
import { useCartStore } from '../store/cart'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const activityId = computed(() => route.params.id)
const activity = ref(null)
const product = ref(null)
const remaining = ref(null)
const loading = ref(true)
const loadError = ref(false)
const seckillLoading = ref(false)
const now = ref(Date.now())
let timer = null

const productName = computed(() =>
  (activity.value && activity.value.activityName) ||
  (product.value && product.value.name) ||
  '秒杀商品'
)

const countdownTargetMs = computed(() => {
  if (!activity.value) return 0
  const start = new Date(activity.value.startTime).getTime()
  const end = new Date(activity.value.endTime).getTime()
  return now.value < start ? start : end
})

const countdownMs = computed(() => Math.max(0, countdownTargetMs.value - now.value))
const isLongRunning = computed(() => countdownMs.value >= 24 * 3600000)

const countdownLabel = computed(() => {
  if (!activity.value) return ''
  if (isLongRunning.value) return '活动进行中'
  const start = new Date(activity.value.startTime).getTime()
  return now.value < start ? '距开始' : '距结束'
})

const hours = computed(() => String(Math.floor(countdownMs.value / 3600000)).padStart(2, '0'))
const minutes = computed(() => String(Math.floor((countdownMs.value % 3600000) / 60000)).padStart(2, '0'))
const seconds = computed(() => String(Math.floor((countdownMs.value % 60000) / 1000)).padStart(2, '0'))
const isUrgent = computed(() => countdownMs.value > 0 && countdownMs.value < 3600000)

const isBeforeStart = computed(() => {
  if (!activity.value) return false
  return now.value < new Date(activity.value.startTime).getTime()
})

const isEnded = computed(() => {
  if (!activity.value) return false
  return now.value >= new Date(activity.value.endTime).getTime()
})

const soldOut = computed(() => remaining.value !== null && remaining.value <= 0)

const soldCount = computed(() => {
  if (!activity.value || remaining.value === null) return 0
  return Math.max(0, activity.value.totalStock - remaining.value)
})

const soldPercent = computed(() => {
  if (!activity.value || !activity.value.totalStock) return 0
  return Math.min(100, Math.round((soldCount.value / activity.value.totalStock) * 100))
})

const progressColor = computed(() => {
  if (soldPercent.value >= 80) return '#ff4d2e'
  if (soldPercent.value >= 50) return '#ffb300'
  return '#ff7a00'
})

const remainingText = computed(() => (remaining.value === null ? '--' : remaining.value))

const buttonDisabled = computed(() =>
  loading.value ||
  !activity.value ||
  isBeforeStart.value ||
  isEnded.value ||
  soldOut.value ||
  seckillLoading.value
)

const buttonText = computed(() => {
  if (loading.value || !activity.value) return '加载中...'
  if (isBeforeStart.value) return '未开始'
  if (isEnded.value) return '已结束'
  if (soldOut.value) return '已抢光'
  return '立即抢购'
})

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const [activityRes, stockRes] = await Promise.all([
      getActivity(activityId.value),
      getInventoryStock(activityId.value)
    ])
    if (activityRes.data.code !== 200 || !activityRes.data.data) {
      throw new Error('activity load failed')
    }
    activity.value = activityRes.data.data
    if (stockRes.data.code === 200) {
      remaining.value = stockRes.data.data
    }
    const productRes = await getProduct(activity.value.productId)
    if (productRes.data.code === 200) {
      product.value = productRes.data.data || null
    }
  } catch (error) {
    loadError.value = true
    ElMessage.error('商品信息加载失败，请返回重试')
  } finally {
    loading.value = false
  }
}

async function handleSeckill() {
  if (seckillLoading.value) return
  if (!authStore.userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  seckillLoading.value = true
  try {
    const res = await seckill(activityId.value)
    if (res.data && res.data.code === 200) {
      const order = res.data.data || {}
      ElMessage.success('抢购成功！订单号: ' + (order.orderNo || '已生成'))
      const stockRes = await getInventoryStock(activityId.value)
      if (stockRes.data.code === 200) {
        remaining.value = stockRes.data.data
      }
    } else {
      ElMessage.error((res.data && res.data.message) || '抢购失败')
    }
  } catch (error) {
    ElMessage.error('系统繁忙，请稍后重试')
  } finally {
    seckillLoading.value = false
  }
}

function addToCart() {
  if (!activity.value) return
  cartStore.add({
    id: activity.value.id,
    t: 'flash',
    productId: activity.value.productId,
    activityName: productName.value,
    flashPrice: activity.value.flashPrice,
    totalStock: activity.value.totalStock
  })
  ElMessage.success('已加入购物车')
}

onMounted(() => {
  loadData()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.flash-detail {
  max-width: 1000px;
  margin: 20px auto;
  padding: 28px;
  background: #fff;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}

.product-image {
  width: 100%;
  height: 400px;
  border-radius: var(--r-md);
  background: #f4efeb;
}

.image-placeholder {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4efeb;
  color: var(--ink-3);
  font-size: 56px;
  font-weight: 700;
}

.detail-info {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.flag-row {
  margin-bottom: 10px;
}

.flag {
  display: inline-block;
  background: linear-gradient(135deg, #ff7a00, #ff4d2e);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 6px;
  letter-spacing: 1px;
}

.product-name {
  font-size: 26px;
  line-height: 1.35;
  margin: 0 0 16px;
  color: var(--ink);
  font-weight: 700;
}

.price-panel {
  background: var(--brand-soft);
  border-radius: var(--r-md);
  padding: 14px 16px;
  margin-bottom: 16px;
}

.price-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-sym {
  font-size: 20px;
  color: var(--brand);
  font-weight: 700;
}

.flash-price {
  font-size: 38px;
  color: var(--brand);
  font-weight: 800;
  line-height: 1;
}

.original-price {
  font-size: 16px;
  color: var(--ink-3);
  text-decoration: line-through;
}

.countdown-row {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}

.countdown-label {
  font-size: 14px;
  color: var(--ink-2);
  margin-right: 10px;
  font-weight: 600;
}

.time-block {
  background: #1a1412;
  color: #fff;
  padding: 5px 9px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 18px;
  min-width: 38px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.time-block.urgent {
  background: var(--brand);
}

.colon {
  margin: 0 5px;
  font-weight: 700;
  color: var(--brand);
}

.running-badge {
  background: var(--green);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 6px;
}

.stock-row {
  margin-bottom: 24px;
}

.stock-text {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--ink-2);
}

.stock-text b {
  color: var(--brand);
  font-weight: 700;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: auto;
}

.seckill-btn {
  flex: 1;
  border-radius: 10px;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 1px;
  height: 46px;
}

.cart-btn {
  border-radius: 10px;
  height: 46px;
}

.state-box {
  max-width: 1000px;
  margin: 40px auto;
  background: #fff;
  border-radius: var(--r-md);
  padding: 20px;
  border: 1px solid var(--line);
}

@media (max-width: 768px) {
  .product-image,
  .image-placeholder {
    height: 280px;
  }
  .flash-detail {
    padding: 20px;
  }
}
</style>
