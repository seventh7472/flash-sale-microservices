<template>
  <div>
    <el-input placeholder="搜索商品" v-model="q" clearable size="large" style="margin-bottom:16px" @input="f" />
    <div style="display:flex;gap:8px;overflow-x:auto;margin-bottom:16px">
      <div v-for="c in cs" :key="c.id" class="cat-tag" :class="{ active: a === c.id }" @click="a = c.id; f()">{{ c.label }}</div>
    </div>
    <el-carousel :interval="4000" height="160px" style="border-radius:10px;overflow:hidden;margin-bottom:16px">
      <el-carousel-item v-for="(s,i) in slides" :key="i">
        <div :style="'height:100%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:20px;font-weight:700;background:linear-gradient(135deg,'+s.c1+','+s.c2+')'">{{ s.text }}</div>
      </el-carousel-item>
    </el-carousel>
    <div style="font-size:13px;color:#999;margin-bottom:12px">{{ list.length }}件商品</div>
    <el-empty v-if="!list.length" description="暂无数据" />
    <div class="card-grid">
      <div v-for="item in list" :key="item.key" class="flash-card" @click="open(item)">
        <el-image class="card-img" :src="item.product?.image" fit="cover" lazy>
          <template #placeholder>
            <div class="img-ph">加载中</div>
          </template>
          <template #error>
            <div class="img-err">{{ firstChar(item.product?.name || item.name) }}</div>
          </template>
        </el-image>
        <div class="card-body">
          <el-tag v-if="item.type === 'activity'" type="danger" effect="dark" size="small" style="align-self:flex-start;margin-bottom:6px">限时秒杀</el-tag>
          <div class="card-name">{{ item.name }}</div>
          <div class="card-price">¥{{ item.price }}<span v-if="item.origin" class="card-origin">¥{{ item.origin }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listActivities, listProducts } from '@/api'

const all = ref([]), list = ref([]), q = ref(''), a = ref(0)
const router = useRouter()
const cs = [
  { id: 0, label: '全部' },
  { id: 1, label: '数码' },
  { id: 2, label: '家电' },
  { id: 3, label: '运动' },
  { id: 4, label: '美妆' },
  { id: 5, label: '食品' },
  { id: 6, label: '图书' },
  { id: 7, label: '其他' }
]
const slides = [
  { text: '618 年中大促 全场5折起', c1: '#1a73e8', c2: '#0d47a1' },
  { text: '每日精选 限时秒杀 手慢无', c1: '#00bcd4', c2: '#0097a7' },
  { text: '品质好物 低至1折 速来抢', c1: '#7c4dff', c2: '#536dfe' }
]

function firstChar(n) {
  return (n || '?').trim().charAt(0)
}

function catOf(item) {
  const d = item.product?.description
  return d ? d : '其他'
}

function f() {
  let r = all.value
  if (q.value) {
    const s = q.value.toLowerCase()
    r = r.filter(x => (x.name || '').toLowerCase().includes(s))
  }
  if (a.value > 0) {
    const label = cs[a.value].label
    r = r.filter(x => catOf(x) === label)
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
    const [ar, pr] = await Promise.all([listActivities(), listProducts(1, 100)])
    const actMap = {}
    if (ar.data.code === 200) {
      ;(ar.data.data || []).forEach(act => { actMap[act.productId] = act })
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
.cat-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  background: #eee;
  color: #333;
  border: 1px solid transparent;
  transition: all .2s;
  flex-shrink: 0;
}
.cat-tag.active {
  background: #fff;
  color: #f53f2d;
  border-color: #f53f2d;
  font-weight: 600;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 10px;
  align-items: stretch;
}
.flash-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: transform .15s, box-shadow .15s;
}
.flash-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.10);
}
.card-img {
  width: 100%;
  height: 200px;
  display: block;
}
.img-ph,
.img-err {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #999;
  font-size: 14px;
}
.img-err {
  background: #eee;
  color: #666;
  font-size: 28px;
  font-weight: 600;
}
.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 8px 10px 10px;
}
.card-name {
  font-size: 12px;
  color: #333;
  margin-bottom: 6px;
  height: 36px;
  line-height: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-price {
  margin-top: auto;
  font-size: 18px;
  font-weight: 700;
  color: #f53f2d;
  height: 24px;
  line-height: 24px;
}
.card-origin {
  font-size: 11px;
  color: #bbb;
  text-decoration: line-through;
  margin-left: 4px;
  font-weight: 400;
}
</style>
