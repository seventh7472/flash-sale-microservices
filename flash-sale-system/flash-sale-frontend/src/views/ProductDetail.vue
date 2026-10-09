<template>
  <div>
    <div v-if="product" class="product-detail">
      <el-row :gutter="28">
        <el-col :xs="24" :sm="10">
          <el-image :src="product.image" fit="cover" class="product-image" lazy>
            <template #error>
              <div class="image-placeholder">{{ productName.slice(0, 1) }}</div>
            </template>
          </el-image>
        </el-col>
        <el-col :xs="24" :sm="14">
          <div class="detail-info">
            <h1 class="product-name">{{ product.name }}</h1>

            <div class="price-panel">
              <div class="price-main">
                <span class="price-sym">¥</span>
                <span class="product-price">{{ product.originalPrice }}</span>
                <span class="tag-plain">普通商品</span>
              </div>
            </div>

            <p v-if="product.category" class="product-desc"><span class="desc-label">分类</span>{{ product.category }}</p>
            <p v-if="product.description" class="product-desc"><span class="desc-label">描述</span>{{ product.description }}</p>

            <div class="action-row">
              <el-button type="primary" size="large" class="buy-btn" @click="buyNow">立即购买</el-button>
              <el-button size="large" class="cart-btn" @click="addToCart">加入购物车</el-button>
              <el-button size="large" @click="router.push('/')">返回首页</el-button>
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

.product-price {
  font-size: 38px;
  color: var(--brand);
  font-weight: 800;
  line-height: 1;
}

.tag-plain {
  font-size: 12px;
  color: var(--ink-3);
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 2px 8px;
}

.product-desc {
  font-size: 14px;
  color: var(--ink-2);
  margin-bottom: 12px;
  line-height: 1.7;
}

.desc-label {
  display: inline-block;
  min-width: 40px;
  color: var(--ink-3);
  margin-right: 8px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: auto;
  flex-wrap: wrap;
}

.buy-btn {
  flex: 1;
  border-radius: 10px;
  font-weight: 700;
  font-size: 16px;
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
  .product-detail {
    padding: 20px;
  }
}
</style>
