<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">活动管理</h2>
      <el-button type="primary" @click="show=true">新建活动</el-button>
    </div>
    <div class="table-card" v-if="acts.length">
      <el-table :data="acts" style="width:100%">
      <el-table-column prop="activityName" label="名称" />
      <el-table-column label="秒杀价"><template #default="{row}">¥{{ row.flashPrice }}</template></el-table-column>
      <el-table-column label="时间"><template #default="{row}">{{ row.startTime?.replace('T',' ')?.slice(0,16) }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{row}"><el-tag :type="['info','success','warning','danger'][row.status]">{{ ['待开始','进行中','已结束','已取消'][row.status] }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{row}"><el-button size="small" @click="warm(row)" :loading="warmingId===row.id" :disabled="row.status!==0 || warmingId!==null">预热</el-button></template>
      </el-table-column>
    </el-table>
    </div>
    <el-empty v-else description="暂无数据" />
    <el-dialog v-model="show" title="新建活动" width="520" @open="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="商品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择商品" filterable style="width:100%" :loading="productLoading">
            <el-option v-for="p in products" :key="p.id" :label="`${p.name}（¥${p.originalPrice}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" prop="flashPrice">
          <el-input-number v-model="form.flashPrice" :min="0.01" :precision="2" :step="1" style="width:100%" placeholder="请输入秒杀价" />
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="1" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="限购数量" prop="limitQuantity">
          <el-input-number v-model="form.limitQuantity" :min="1" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="QPS上限" prop="qpsLimit">
          <el-input-number v-model="form.qpsLimit" :min="1" :step="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" placeholder="请选择开始时间" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" placeholder="请选择结束时间" />
        </el-form-item>
        <el-form-item label="库存预热">
          <el-checkbox v-model="form.autoWarmUp">创建后立即预热库存</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="show=false">取消</el-button><el-button type="primary" :loading="creating" @click="create">创建</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { listAllActivities, listProducts, createActivity, warmUpStock } from '@/api'
import { ElMessage } from 'element-plus'

const show = ref(false), acts = ref([])
const creating = ref(false)
const warmingId = ref(null)
const formRef = ref(null)
const products = ref([])
const productLoading = ref(false)

const form = reactive({
  activityName: '', productId: null, flashPrice: null, totalStock: 100,
  limitQuantity: 1, qpsLimit: 100, startTime: '', endTime: '', autoWarmUp: false
})

function pad(n) { return String(n).padStart(2, '0') }
function fmt(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
function defaultTimes() {
  const start = new Date()
  start.setDate(start.getDate() + 1)
  start.setHours(10, 0, 0, 0)
  const end = new Date(start.getTime() + 2 * 3600 * 1000)
  return { startTime: fmt(start), endTime: fmt(end) }
}
function resetForm() {
  const t = defaultTimes()
  Object.assign(form, {
    activityName: '', productId: null, flashPrice: null, totalStock: 100,
    limitQuantity: 1, qpsLimit: 100, startTime: t.startTime, endTime: t.endTime, autoWarmUp: false
  })
  formRef.value?.clearValidate()
}

const rules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  flashPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }],
  limitQuantity: [{ required: true, message: '请输入限购数量', trigger: 'blur' }],
  qpsLimit: [{ required: true, message: '请输入QPS上限', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: (rule, value, callback) => {
        if (value && form.startTime && value <= form.startTime) callback(new Error('结束时间必须晚于开始时间'))
        else callback()
      }, trigger: 'change' }
  ]
}

watch(() => form.startTime, () => formRef.value?.validateField('endTime').catch(() => {}))

async function load() {
  try {
    const r = await listAllActivities()
    if (r.data.code === 200) acts.value = r.data.data || []
  } catch {}
}
async function loadProducts() {
  productLoading.value = true
  try {
    const r = await listProducts(1, 100)
    if (r.data.code === 200) products.value = r.data.data?.records || []
  } catch {} finally { productLoading.value = false }
}

onMounted(() => { load(); loadProducts() })

async function create() {
  if (creating.value) return
  try { await formRef.value.validate() } catch { return }
  creating.value = true
  try {
    const payload = { ...form }
    delete payload.autoWarmUp
    const r = await createActivity(payload, form.autoWarmUp)
    if (r.data.code !== 200) throw new Error(r.data.message || '创建失败')
    ElMessage.success(form.autoWarmUp ? '创建成功，已自动预热库存' : '创建成功')
    show.value = false
    await load()
  } catch (e) {
    if (!e?.response && e?.message) ElMessage.error(e.message)
  } finally { creating.value = false }
}
async function warm(a) {
  if (warmingId.value) return
  warmingId.value = a.id
  try {
    await warmUpStock(a.id, a.totalStock)
    ElMessage.success('预热成功')
  } catch {} finally { warmingId.value = null }
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  margin: 0;
  color: var(--ink);
  font-weight: 700;
}

.table-card {
  background: #fff;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  padding: 8px;
  overflow: hidden;
}
</style>