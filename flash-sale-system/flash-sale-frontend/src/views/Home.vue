<template>
  <div>
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        placeholder="搜索商品"
        v-model="q"
        clearable
        size="large"
        @input="f"
      >
        <template #prefix>
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
            <path d="M15.5 14h-.79l-.28-.27a6.5 6.5 0 1 0-.7.7l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z" fill="currentColor" />
          </svg>
        </template>
      </el-input>
    </div>

    <!-- 分类标签 -->
    <div class="cat-bar">
      <div class="cat-tag" :class="{ active: a === '' }" @click="a = ''; f()">全部</div>
      <div v-for="c in cs" :key="c" class="cat-tag" :class="{ active: a === c }" @click="a = c; f()">{{ c }}</div>
    </div>

    <!-- 轮播 -->
    <el-carousel :interval="4000" height="200px" class="hero" arrow="hover" indicator-position="none">
      <el-carousel-item v-for="(s, i) in slides" :key="i">
        <div class="hero-slide" :style="{ background: `linear-gradient(120deg, ${s.c1}, ${s.c2})` }">
          <div class="hero-text">
            <div class="hero-tag">限时秒杀</div>
            <div class="hero-title">{{ s.title }}</div>
            <div class="hero-sub">{{ s.sub }}</div>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 商品列表 -->
    <div class="list-head">
      <span class="list-count">{{ list.length }} 件商品</span>
    </div>
    <el-empty v-if="!list.length" description="暂无数据" />
    <div class="card-grid">
      <div v-for="item in list" :key="item.key" class="flash-card" @click="open(item)">
        <div class="card-img-wrap">
          <el-image class="card-img" :src="item.product?.image" fit="cover" lazy>
            <template #placeholder>
              <div class="img-ph">加载中</div>
            </template>
            <template #error>
              <div class="img-err">{{ firstChar(item.product?.name || item.name) }}</div>
            </template>
          </el-image>
          <span v-if="item.type === 'activity'" class="corner-flag">秒杀</span>
        </div>
        <div class="card-body">
          <div class="card-name">{{ item.name }}</div>
          <div class="card-price">
            <span class="price-sym">¥</span>{{ item.price }}
            <span v-if="item.origin" class="card-origin">¥{{ item.origin }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listActivities, listProducts, listCategories } from '@/api'

const all = ref([]), list = ref([]), q = ref(''), a = ref('')
const router = useRouter()
const cs = ref([])
const slides = [
  { title: '618 年中大促', sub: '全场 5 折起 · 手慢无', c1: '#ff7a00', c2: '#ff4d2e' },
  { title: '每日精选秒杀', sub: '爆款好物限时抢', c1: '#ff4d2e', c2: '#c81e0f' },
  { title: '品质好物', sub: '低至 1 折 · 速来抢', c1: '#8b1eff', c2: '#4d2eff' }
]

function firstChar(n) {
  return (n || '?').trim().charAt(0)
}

function catOf(item) {
  return item.product?.category || '其他'
}

function f() {
  let r = all.value
  if (q.value) {
    const s = q.value.toLowerCase()
    r = r.filter(x => (x.name || '').toLowerCase().includes(s))
  }
  if (a.value) {
    r = r.filter(x => catOf(x) === a.value)
  }
  list.value = r
}

function open(item) {
  if (item.type === 'activity') {
    router.push('/flash/' + item.actId)
    return
  }
  router.push('/product/' + item.product.id)
}

onMounted(async () => {
  try {
    const [ar, pr, cr] = await Promise.all([listActivities(), listProducts(1, 100), listCategories()])
    const actMap = {}
    if (ar.data.code === 200) {
      ;(ar.data.data || []).forEach(act => { actMap[act.productId] = act })
    }
    if (cr.data.code === 200) {
      cs.value = cr.data.data || []
    }
    if (pr.data.code === 200) {
      all.value = (pr.data.data.records || []).map(p => {
        const act = actMap[p.id]
        return {
          key: act ? 'a' + act.id : 'p' + p.id,
          type: act ? 'activity' : 'product',
          actId: act ? act.id : null,
          product: p,
          name: act ? act.activityName : p.name,
          price: act ? act.flashPrice : p.originalPrice,
          origin: act ? p.originalPrice : null
        }
      })
      all.value.sort((x, y) => (x.type === y.type ? 0 : x.type === 'activity' ? -1 : 1))
    }
    f()
  } catch {}
})
</script>
<style scoped>
.search-bar {
  margin-bottom: 14px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 1px 0 0 var(--line) inset;
  padding: 4px 16px;
}

.cat-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
  padding-bottom: 2px;
}

.cat-tag {
  padding: 7px 18px;
  border-radius: 20px;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  background: #fff;
  color: var(--ink-2);
  border: 1px solid var(--line);
  transition: all 0.2s;
  flex-shrink: 0;
}

.cat-tag:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.cat-tag.active {
  background: var(--brand);
  color: #fff;
  border-color: var(--brand);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255, 77, 46, 0.28);
}

.hero {
  border-radius: var(--r-lg);
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
}

.hero-slide {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 48px;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-tag {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: 12px;
  letter-spacing: 1px;
}

.hero-title {
  color: #fff;
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.hero-sub {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.list-count {
  font-size: 13px;
  color: var(--ink-3);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  align-items: stretch;
}

.flash-card {
  background: #fff;
  border-radius: var(--r-md);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: transform 0.2s, box-shadow 0.2s;
}

.flash-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}

.card-img-wrap {
  position: relative;
}

.card-img {
  width: 100%;
  height: 190px;
  display: block;
}

.corner-flag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(255, 77, 46, 0.4);
}

.img-ph,
.img-err {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4efeb;
  color: var(--ink-3);
  font-size: 14px;
}

.img-err {
  font-size: 32px;
  font-weight: 700;
  color: var(--ink-2);
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px 12px 12px;
}

.card-name {
  font-size: 13px;
  color: var(--ink);
  margin-bottom: 8px;
  line-height: 1.5;
  min-height: 39px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price {
  margin-top: auto;
  font-size: 20px;
  font-weight: 800;
  color: var(--brand);
  line-height: 1;
}

.price-sym {
  font-size: 13px;
  font-weight: 700;
}

.card-origin {
  font-size: 12px;
  color: var(--ink-3);
  text-decoration: line-through;
  margin-left: 4px;
  font-weight: 400;
}
</style>
