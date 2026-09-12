<template>
  <div v-if="order" style="max-width:600px;margin:0 auto">
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:20px">
      <span style="font-size:18px;cursor:pointer" @click="$router.back()">&#8592;</span>
      <span style="font-size:16px;font-weight:600">订单详情</span>
    </div>
    <el-card style="margin-bottom:16px">
      <div style="font-size:13px;color:#666;margin-bottom:8px">订单号: {{ order.orderNo }}</div>
      <div style="font-size:14px;font-weight:600;margin-bottom:8px">{{ order.activityName || (order.activityId ? '秒杀商品' : '普通商品') }}</div>
      <div style="font-size:20px;font-weight:700;color:#f53f2d">¥{{ order.totalPrice }}</div>
      <div style="margin-top:12px"><el-tag :type="['warning','success','info','danger'][order.status]">{{ ['待支付','已支付','已取消','已退款'][order.status] }}</el-tag></div>
      <div style="font-size:12px;color:#999;margin-top:8px">{{ (order.createTime||'').replace('T',' ') }}</div>
    </el-card>
    <el-button style="width:100%" @click="$router.push('/orders')">返回订单列表</el-button>
  </div>
  <div v-else-if="notFound" style="text-align:center;padding:60px 0">
    <el-empty description="订单不存在或无权查看">
      <el-button type="primary" @click="$router.push('/orders')">返回订单列表</el-button>
    </el-empty>
  </div>
  <div v-else style="text-align:center;padding:60px 0;color:#999">加载中...</div>
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
      // 秒杀订单补充活动名称展示
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
