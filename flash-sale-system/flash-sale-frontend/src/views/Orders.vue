<template>
  <div>
    <h2 class="page-title">我的订单</h2>
    <div class="orders-card" v-if="orders.length">
      <el-table :data="orders" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/order/' + row.orderNo)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="['warning', 'success', 'info', 'danger'][row.status]">{{ ['待支付', '已支付', '已取消', '已退款'][row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" :loading="payingNo === row.orderNo" :disabled="!!payingNo || !!cancelingNo" @click="pay(row.orderNo)">支付</el-button>
            <el-button v-if="row.status === 0" size="small" :loading="cancelingNo === row.orderNo" :disabled="!!payingNo || !!cancelingNo" @click="cancel(row.orderNo)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-empty v-else description="暂无订单" />
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { listOrders, payOrder, cancelOrder } from '@/api'
import { ElMessage } from 'element-plus'
const orders = ref([])
const payingNo = ref(null)
const cancelingNo = ref(null)
onMounted(async () => {
  try {
    const r = await listOrders()
    if (r.data.code === 200) orders.value = r.data.data || []
  } catch {}
})
async function pay(no) {
  if (payingNo.value || cancelingNo.value) return
  payingNo.value = no
  try {
    const r = await payOrder(no)
    if (r.data.code !== 200) throw new Error(r.data.message || '支付失败')
    ElMessage.success('支付成功')
    const l = await listOrders()
    if (l.data.code === 200) orders.value = l.data.data
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  } finally { payingNo.value = null }
}
async function cancel(no) {
  if (payingNo.value || cancelingNo.value) return
  cancelingNo.value = no
  try {
    const r = await cancelOrder(no)
    if (r.data.code !== 200) throw new Error(r.data.message || '取消失败')
    ElMessage.success('已取消')
    const l = await listOrders()
    if (l.data.code === 200) orders.value = l.data.data
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  } finally { cancelingNo.value = null }
}
</script>
<style scoped>
.page-title {
  font-size: 20px;
  margin: 0 0 16px;
  color: var(--ink);
  font-weight: 700;
}

.orders-card {
  background: #fff;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  padding: 8px;
  overflow: hidden;
}

.amount {
  font-weight: 700;
  color: var(--brand);
}
</style>
