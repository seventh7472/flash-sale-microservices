<template>
  <div class="cheat-page">
    <div class="page-head">
      <div>
        <h2>AI 防作弊监控</h2>
        <p>{{ mode === 'empty' ? '暂无实时可疑数据（黑名单为真实记录）' : '可疑用户与黑名单实时概览' }}</p>
      </div>
      <el-button type="primary" plain :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div class="page-body">
      <div class="page-main">
        <div class="stats">
          <div class="stat stat-requests">
            <span class="stat-label">总请求数</span>
            <span class="stat-value">{{ totalRequests.toLocaleString() }}</span>
            <span class="stat-hint">可疑用户累计</span>
          </div>
          <div class="stat stat-suspicious">
            <span class="stat-label">可疑用户数</span>
            <span class="stat-value">{{ suspicious.length }}</span>
            <span class="stat-hint">待处理</span>
          </div>
          <div class="stat stat-banned">
            <span class="stat-label">封禁用户数</span>
            <span class="stat-value">{{ blacklist.length }}</span>
            <span class="stat-hint">黑名单累计</span>
          </div>
        </div>

        <el-card class="panel" shadow="never">
          <div class="panel-head">
            <span>可疑用户</span>
            <el-tag :type="mode === 'empty' ? 'info' : 'success'" size="small">
              {{ mode === 'empty' ? '暂无数据' : '实时数据' }}
            </el-tag>
          </div>
          <el-table :data="suspicious" v-loading="loading">
            <template #empty><el-empty description="暂无数据" /></template>
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="username" label="用户名" min-width="120">
              <template #default="{ row }">{{ row.username || '-' }}</template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" min-width="140" />
            <el-table-column prop="requestCount" label="请求次数" width="110" sortable />
            <el-table-column label="风险评分" width="220" sortable :sort-by="row => row.riskScore">
              <template #default="{ row }">
                <div class="risk-cell">
                  <el-progress
                    :percentage="Math.min(Number(row.riskScore) || 0, 100)"
                    :stroke-width="8"
                    :show-text="false"
                    :color="riskColor(row.riskScore)"
                    style="width: 90px"
                  />
                  <span class="risk-text">{{ row.riskScore }}</span>
                  <el-tag :type="riskType(row.riskScore)" size="small">{{ riskLevel(row.riskScore) }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="检测理由" min-width="260">
              <template #default="{ row }">
                <div class="reason-cell">
                  <el-tag v-if="row.aiAnalysis" type="warning" size="small" effect="plain" class="reason-tag">AI</el-tag>
                  <span class="reason-text">{{ row.aiAnalysis || row.reason || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="!row.banned"
                  type="danger"
                  size="small"
                  :loading="banningId === row.userId"
                  :disabled="String(row.userId) === myUserId"
                  @click="ban(row)"
                >
                  {{ String(row.userId) === myUserId ? '本人' : '封禁' }}
                </el-button>
                <el-tag v-else type="info" size="small">已封禁</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="panel" shadow="never">
          <div class="panel-head">
            <span>黑名单</span>
            <span class="panel-count">{{ blacklist.length }} 条</span>
          </div>
          <el-table :data="blacklist" v-loading="loading" empty-text="暂无黑名单记录">
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column label="用户名" min-width="110">
              <template #default="{ row }">{{ row.username || '-' }}</template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" min-width="140">
              <template #default="{ row }">{{ row.ip || '-' }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="封禁原因" min-width="180">
              <template #default="{ row }">{{ row.reason || '-' }}</template>
            </el-table-column>
            <el-table-column prop="riskScore" label="风险分" width="90" sortable />
            <el-table-column label="过期时间" min-width="160">
              <template #default="{ row }">{{ row.expireTime || row.createTime || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <aside class="chat-panel">
        <div class="chat-head">
          <span class="chat-title">
            <svg class="chat-title-icon" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
              <path d="M12 2l1.9 5.1L19 9l-5.1 1.9L12 16l-1.9-5.1L5 9l5.1-1.9L12 2z" fill="currentColor" />
              <path d="M18.5 14l.9 2.4 2.4.9-2.4.9-.9 2.4-.9-2.4-2.4-.9 2.4-.9.9-2.4z" fill="currentColor" opacity="0.6" />
            </svg>
            AI 风控助手
          </span>
          <el-button link size="small" :disabled="!messages.length || chatLoading" @click="clearChat">清空</el-button>
        </div>

        <div ref="chatBodyRef" class="chat-body">
          <div v-if="!messages.length" class="chat-empty">
            <p>可以这样问我：</p>
            <span class="chat-tip" @click="chatInput = '当前有哪些可疑用户'">当前有哪些可疑用户</span>
            <span class="chat-tip" @click="chatInput = '需要封禁哪些用户'">需要封禁哪些用户</span>
            <span class="chat-tip" @click="chatInput = '现在系统压力大吗'">现在系统压力大吗</span>
          </div>

          <div
            v-for="(m, i) in messages"
            :key="i"
            class="chat-row"
            :class="m.role === 'user' ? 'is-user' : 'is-ai'"
          >
            <div class="bubble" :class="[m.role === 'user' ? 'bubble-user' : 'bubble-ai', { 'bubble-error': m.failed }]">
              <div v-if="m.degraded" class="bubble-tag">降级回答</div>
              <div class="bubble-text">{{ m.content }}</div>
            </div>
          </div>

          <div v-if="chatLoading" class="chat-row is-ai">
            <div class="bubble bubble-ai">
              <span class="typing"><i></i><i></i><i></i></span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="200"
            placeholder="输入问题，Enter 发送"
            :disabled="chatLoading"
            @keydown.enter.exact.prevent="sendChat"
          />
          <el-button type="primary" :loading="chatLoading" :disabled="!chatInput.trim()" @click="sendChat">
            发送
          </el-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCheatUsers, listCheatBlacklist, banCheatUser } from '@/api'

const mode = ref('empty')
const myUserId = localStorage.getItem('userId') || ''

const loading = ref(false)
const banningId = ref(null)
const suspicious = ref([])
const blacklist = ref([])
const totalRequests = ref(0)

// ─────────────── AI 风控助手对话 ───────────────
const chatInput = ref('')
const chatLoading = ref(false)
const messages = ref([])
const chatBodyRef = ref(null)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [usersRes, blacklistRes] = await Promise.allSettled([listCheatUsers(), listCheatBlacklist()])
    const users = usersRes.status === 'fulfilled' && usersRes.value.data?.code === 200 ? usersRes.value.data.data || [] : []
    const bans = blacklistRes.status === 'fulfilled' && blacklistRes.value.data?.code === 200 ? blacklistRes.value.data.data || [] : []
    mode.value = users.length ? 'real' : 'empty'
    suspicious.value = users || []
    blacklist.value = bans
    totalRequests.value = users.reduce((sum, user) => sum + (Number(user.requestCount) || 0), 0)
  } catch {
    mode.value = 'empty'
    suspicious.value = []
  } finally {
    loading.value = false
  }
}

async function ban(row) {
  if (String(row.userId) === myUserId) return
  try {
    await ElMessageBox.confirm(`确定封禁用户 ${row.userId} 吗？封禁后 24 小时内无法参与秒杀。`, '封禁确认', { type: 'warning', confirmButtonText: '封禁', cancelButtonText: '取消' })
  } catch {
    return
  }
  banningId.value = row.userId
  try {
    const res = await banCheatUser(row.userId)
    if (res.data?.code === 200) {
      ElMessage.success(`用户 ${row.userId} 已封禁`)
      loadData()
      return
    }
    ElMessage.error(res.data?.message || '封禁失败')
  } catch {
    ElMessage.error('封禁失败，请稍后重试')
  } finally {
    banningId.value = null
  }
}

// 发送提问：POST /api/ai/chat（网关要求 ADMIN token，这里手动带上）
async function sendChat() {
  const question = chatInput.value.trim()
  if (!question || chatLoading.value) return

  messages.value.push({ role: 'user', content: question })
  chatInput.value = ''
  chatLoading.value = true
  await scrollChat()

  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ question })
    })
    const body = await res.json().catch(() => null)
    if (!res.ok || !body || body.code !== 200) {
      const hint = body?.message || (res.status === 401 ? '登录已过期，请重新登录' : `HTTP ${res.status}`)
      messages.value.push({ role: 'ai', content: `提问失败：${hint}`, failed: true })
      return
    }
    const data = body.data || {}
    messages.value.push({
      role: 'ai',
      content: data.answer || '（未返回内容）',
      degraded: !!data.degraded
    })
  } catch (e) {
    messages.value.push({ role: 'ai', content: `网络异常：${e?.message || '请求未送达'}`, failed: true })
  } finally {
    chatLoading.value = false
    await scrollChat()
  }
}

function clearChat() {
  messages.value = []
}

async function scrollChat() {
  await nextTick()
  const el = chatBodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function riskLevel(score) {
  const value = Number(score) || 0
  if (value >= 60) return '高危'
  if (value >= 30) return '中危'
  return '低危'
}

function riskType(score) {
  const value = Number(score) || 0
  if (value >= 60) return 'danger'
  if (value >= 30) return 'warning'
  return 'success'
}

function riskColor(score) {
  const value = Number(score) || 0
  if (value >= 60) return '#f56c6c'
  if (value >= 30) return '#e6a23c'
  return '#67c23a'
}
</script>

<style scoped>
.cheat-page {
  min-width: 0;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  line-height: 1.3;
  color: #1f2937;
}

.page-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

/* 左右两栏：左侧数据区 + 右侧 AI 对话框 */
.page-body {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.page-main {
  flex: 1;
  min-width: 0;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-hint {
  font-size: 12px;
  color: #9ca3af;
}

.stat-requests .stat-value {
  color: #2563eb;
}

.stat-suspicious .stat-value {
  color: #d97706;
}

.stat-banned .stat-value {
  color: #dc2626;
}

.panel {
  margin-bottom: 16px;
  border-radius: 8px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.panel-count {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
}

.risk-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reason-cell {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.reason-tag {
  flex: none;
  margin-top: 1px;
}

.reason-text {
  font-size: 13px;
  line-height: 1.6;
  color: #4b5563;
}

.risk-text {
  min-width: 28px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

/* ─────────────── AI 风控助手对话框 ─────────────── */
.chat-panel {
  position: sticky;
  top: 16px;
  display: flex;
  flex: none;
  flex-direction: column;
  width: 340px;
  height: 560px;
  background: #fff;
  border: 1px solid #e8ecf0;
  border-radius: 8px;
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid #eef2f6;
  background: #fafbfc;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.chat-title-icon {
  color: #7c3aed;
}

.chat-body {
  flex: 1;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
  background: #f7f9fc;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px;
  font-size: 13px;
  color: #9ca3af;
}

.chat-empty p {
  margin: 0;
}

.chat-tip {
  align-self: flex-start;
  padding: 4px 10px;
  border: 1px dashed #c7d2fe;
  border-radius: 14px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  cursor: pointer;
}

.chat-tip:hover {
  background: #e0e7ff;
}

.chat-row {
  display: flex;
  margin-bottom: 10px;
}

.chat-row.is-user {
  justify-content: flex-end;
}

.chat-row.is-ai {
  justify-content: flex-start;
}

.bubble {
  max-width: 85%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-user {
  background: #1a73e8;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.bubble-ai {
  background: #fff;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 2px;
}

.bubble-error {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #b91c1c;
}

.bubble-tag {
  display: inline-block;
  margin-bottom: 4px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #fef3c7;
  color: #b45309;
  font-size: 11px;
}

.bubble-text {
  white-space: pre-wrap;
}

.typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 16px;
}

.typing i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #9ca3af;
  animation: typing-blink 1.2s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.2s;
}

.typing i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing-blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
  }
  40% {
    opacity: 1;
  }
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #eef2f6;
  background: #fff;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

@media (max-width: 1100px) {
  .page-body {
    flex-direction: column;
  }

  .chat-panel {
    position: static;
    width: 100%;
    height: 460px;
  }
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: 1fr;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>
