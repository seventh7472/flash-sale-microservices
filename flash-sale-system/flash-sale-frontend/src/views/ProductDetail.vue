<template>
  <div>
    <div v-if="product" class="product-detail">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="10">
          <el-image :src="product.image" fit="cover" class="product-image" lazy>
            <template #error>
              <div class="image-placeholder">{{ productName.slice(0, 1) }}</div>
            </template>
          </el-image>
        </el-col>
        <el-col :xs="24" :sm="14">
          <h1 class="product-name">{{ product.name }}</h1>
          <div class="price-row">
            <span class="product-price">¥{{ product.originalPrice }}</span>
            <el-tag type="primary" effect="plain" size="small">普通商品</el-tag>
          </div>
          <p v-if="product.description" class="product-desc">分类：{{ product.description }}</p>
          <div class="action-row">
            <el-button type="danger" size="large" @click="buyNow">立即购买</el-button>
            <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
            <el-button size="large" @click="router.push('/')">返回首页</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProduct, createNormalOrder } from '../api/index'
import { useCartStore } from '../store/cart'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref(null)
const loadError = ref(false)

const productName = computed(() => (product.value && product.value.name) || '商品')

onMounted(async () => {
  try {
    const res = await getProduct(route.params.id)
    if (res.data.code === 200 && res.data.data) {
      product.value = res.data.data
    } else {
      throw new Error('product load failed')
    }
  } catch (error) {
    loadError.value = true
    ElMessage.error('商品信息加载失败，请返回重试')
  }
})

async function buyNow() {
  if (!localStorage.getItem('userId')) {
    ElMessage.warning('请先登录'); router.push('/login'); return
  }
  if (!product.value) return
  try {
    const r = await createNormalOrder(product.value.id, 1)
    if (r.data.code === 200) {
      ElMessage.success('下单成功，请到“我的订单”支付')
      router.push('/orders')
    } else {
      ElMessage.error(r.data.message || '下单失败')
    }
  } catch { ElMessage.error('下单失败，请稍后重试') }
}

function addToCart() {
  if (!product.value) return
  cartStore.add({
    id: product.value.id,
    t: 'normal',
    productId: product.value.id,
    activityName: product.value.name,
    flashPrice: product.value.originalPrice,
    totalStock: 999
  })
  ElMessage.success('已加入购物车')
}
</script>

<style scoped>
.product-detail {
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
.product-price {
  font-size: 32px;
  color: #e1251b;
  font-weight: bold;
}
.product-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
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
