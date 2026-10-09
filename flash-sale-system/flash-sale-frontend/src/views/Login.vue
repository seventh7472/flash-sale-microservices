<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-head">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
            <path d="M12 1.5 5 12h5l-2 10.5L19 9h-5l3.5-7.5L12 1.5z" fill="currentColor" />
          </svg>
        </div>
        <h2 class="title">{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
        <p class="sub">{{ isRegister ? '注册后即可参与秒杀' : '登录您的账号，抢购好物' }}</p>
      </div>

      <div class="tab-switch">
        <div :class="['tab', { active: tab === 0 }]" @click="tab = 0">密码登录</div>
        <div :class="['tab', { active: tab === 1 }]" @click="tab = 1">验证码登录</div>
      </div>

      <el-form @submit.prevent="handleSubmit" class="form">
        <el-form-item v-if="tab === 0">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large">
            <template #prefix>
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                <path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-3.3 0-8 1.7-8 5v1h16v-1c0-3.3-4.7-5-8-5z" fill="currentColor" />
              </svg>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="tab === 0">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large">
            <template #prefix>
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                <path d="M18 8h-1V6a5 5 0 0 0-10 0v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zm-6 9a2 2 0 1 1 0-4 2 2 0 0 1 0 4z" fill="currentColor" />
              </svg>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="tab === 1">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="tab === 1">
          <div style="display: flex; gap: 8px">
            <el-input v-model="form.code" placeholder="验证码" size="large" maxlength="6" />
            <el-button :disabled="codeSending" size="large" @click="sendCode">{{ codeSending ? codeCount + 's' : '获取验证码' }}</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <label class="remember">
            <input type="checkbox" v-model="remember" /> 记住我
          </label>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" size="large" class="submit-btn" :loading="loading">
            {{ isRegister ? '注册' : (tab === 0 ? '登录' : '验证码登录') }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="foot-link">
        <span v-if="!isRegister">还没有账号？<el-link type="primary" @click="$router.push('/login?reg=1')">立即注册</el-link></span>
        <span v-else>已有账号？<el-link type="primary" @click="isRegister = false; $router.replace('/login')">返回登录</el-link></span>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { register } from '@/api'
import { ElMessage } from 'element-plus'
const router = useRouter(), route = useRoute()
const auth = useAuthStore()
const tab = ref(0)
const loading = ref(false)
const remember = ref(true)
const codeSending = ref(false)
const codeCount = ref(60)
const form = reactive({ username: '', password: '', phone: '', code: '' })
let timer = null
const isRegister = ref(false)

onMounted(() => {
  if (route.query.reg) { isRegister.value = true }
  if (localStorage.getItem('remember')) {
    form.username = localStorage.getItem('savedUser') || ''
    remember.value = true
  }
})

function sendCode() {
  ElMessage.info('验证码登录暂未开放，请使用密码登录')
}

function wx() { ElMessage.info('微信登录暂未开放') }
function ali() { ElMessage.info('支付宝登录暂未开放') }
async function handleSubmit() {
  if (loading.value) return
  if (tab.value === 1) { ElMessage.warning('验证码登录暂未开放，请使用密码登录'); return }
  const username = tab.value === 0 ? form.username : form.phone
  const password = tab.value === 0 ? form.password : form.code
  if (!username || !password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    if (isRegister.value) {
      const r = await register(username, password)
      if (r.data.code !== 200) throw new Error(r.data.message || '注册失败')
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      router.replace('/login')
      return
    }
    await auth.login(username, password)
    if (remember.value) {
      localStorage.setItem('remember', '1')
      localStorage.setItem('savedUser', username)
    } else {
      localStorage.removeItem('remember')
    }
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请稍后重试')
  } finally { loading.value = false }
}
</script>
<style scoped>
.login-wrap {
  min-height: calc(100vh - 100px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: var(--r-lg);
  padding: 36px 32px 28px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--line);
}

.login-head {
  text-align: center;
  margin-bottom: 22px;
}

.logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff7a00, #ff4d2e);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 6px 16px rgba(255, 77, 46, 0.32);
}

.title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 800;
  color: var(--ink);
}

.sub {
  margin: 0;
  font-size: 13px;
  color: var(--ink-3);
}

.tab-switch {
  display: flex;
  margin-bottom: 22px;
  background: #f4efeb;
  border-radius: 10px;
  padding: 4px;
  overflow: hidden;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--ink-2);
  border-radius: 8px;
  transition: all 0.2s;
}

.tab.active {
  background: #fff;
  color: var(--brand);
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

.form :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.remember {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
}

.submit-btn {
  width: 100%;
  border-radius: 10px;
  font-weight: 700;
  letter-spacing: 1px;
}

.foot-link {
  text-align: center;
  font-size: 13px;
  color: var(--ink-3);
  margin-top: 6px;
}
</style>
