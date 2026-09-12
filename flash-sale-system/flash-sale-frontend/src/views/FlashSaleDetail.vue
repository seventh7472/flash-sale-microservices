<template>
  <div>
    <div v-if="activity" class="flash-detail">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="10">
          <el-image :src="product?.image" fit="cover" class="product-image" lazy>
            <template #error>
              <div class="image-placeholder">{{ productName.slice(0, 1) }}</div>
            </template>
          </el-image>
        </el-col>
        <el-col :xs="24" :sm="14">
          <h1 class="product-name">{{ productName }}</h1>
          <div class="price-row">
            <span class="flash-price">¥{{ activity.flashPrice }}</span>
            <span v-if="product?.originalPrice" class="original-price">¥{{ product.originalPrice }}</span>
            <el-tag type="danger" effect="dark" size="small">限时秒杀</el-tag>
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
          </div>
          <div class="stock-row">
            <el-progress :percentage="soldPercent" :stroke-width="14" :color="progressColor" />
            <p class="stock-text">
              已抢 {{ soldCount }} / 共 {{ activity.totalStock }}，剩余 {{ remainingText }}
            </p>
          </div>
          <div class="action-row">
            <el-button type="danger" size="large" :loading="seckillLoading" :disabled="buttonDisabled" @click="handleSeckill">
              {{ buttonText }}
            </el-button>
            <el-button size="large" @click="addToCart">加入购物车</el-button>
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
  if (soldPercent.value >= 80) return '#f56c6c'
  if (soldPercent.value >= 50) return '#e6a23c'
  return '#409eff'
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
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}
.product-image {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}
.image-placeholder {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 56px;
  font-weight: 700;
}
.product-name {
  font-size: 24px;
  margin: 0 0 16px;
}
.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.flash-price {
  font-size: 32px;
  color: #e1251b;
  font-weight: bold;
}
.original-price {
  font-size: 18px;
  color: #999;
  text-decoration: line-through;
}
.countdown-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.countdown-label {
  font-size: 14px;
  color: #666;
  margin-right: 8px;
}
.time-block {
  background: #333;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 18px;
  min-width: 36px;
  text-align: center;
}
.time-block.urgent {
  background: #e1251b;
}
.colon {
  margin: 0 4px;
  font-weight: bold;
  color: #e1251b;
}
.stock-row {
  margin-bottom: 20px;
}
.stock-text {
  margin: 8px 0 0;
  font-size: 13px;
  color: #666;
}
.action-row {
  display: flex;
  gap: 12px;
}
.state-box {
  max-width: 1000px;
  margin: 40px auto;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
</style>
