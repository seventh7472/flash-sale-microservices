<template>
  <div style="max-width:400px;margin:60px auto">
    <el-card>
      <div style="text-align:center;margin-bottom:20px">
        <h2 style="margin:0 0 4px">{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
        <p style="font-size:13px;color:#999;margin:0">{{ isRegister ? '注册后即可参与秒杀' : '登录您的账号' }}</p>
      </div>
      <div style="display:flex;margin-bottom:20px;background:#f5f5f5;border-radius:8px;overflow:hidden">
        <div :style="'flex:1;text-align:center;padding:10px;cursor:pointer;font-size:13px;background:'+(tab===0?'#1a73e8':'transparent')+';color:'+(tab===0?'#fff':'#666')" @click="tab=0">密码登录</div>
        <div :style="'flex:1;text-align:center;padding:10px;cursor:pointer;font-size:13px;background:'+(tab===1?'#1a73e8':'transparent')+';color:'+(tab===1?'#fff':'#666')" @click="tab=1">验证码登录</div>
      </div>
      <el-form @submit.prevent="handleSubmit">
        <el-form-item v-if="tab===0" label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item v-if="tab===0" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item v-if="tab===1" label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="tab===1" label="验证码">
          <div style="display:flex;gap:8px">
            <el-input v-model="form.code" placeholder="验证码" size="large" maxlength="6" />
            <el-button :disabled="codeSending" size="large" @click="sendCode">{{ codeSending ? codeCount+'s' : '获取验证码' }}</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <label style="display:flex;align-items:center;gap:4px;font-size:13px;color:#666">
            <input type="checkbox" v-model="remember" /> 记住我
          </label>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" native-type="submit" size="large" style="width:100%" :loading="loading">{{ isRegister ? '注册' : (tab===0 ? '登录' : '验证码登录') }}</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center;font-size:13px;color:#999;margin-top:8px">
        <span v-if="!isRegister">还没有账号？<el-link type="primary" @click="$router.push('/login?reg=1')">立即注册</el-link></span>
        <span v-else>已有账号？<el-link type="primary" @click="isRegister=false; $router.replace('/login')">返回登录</el-link></span>
      </div>
      <div style="text-align:center;margin-top:24px;padding-top:20px;border-top:1px solid #eee">
        <div style="font-size:12px;color:#999;margin-bottom:14px">其他登录方式</div>
        <div style="display:flex;justify-content:center;gap:20px">
          <div style="width:44px;height:44px;border-radius:50%;background:#07c160;display:flex;align-items:center;justify-content:center;cursor:pointer;font-size:20px;color:#fff" @click="wx"><svg viewBox="0 0 24 24" width="20" height="20" fill="white"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg></div>
          <div style="width:44px;height:44px;border-radius:50%;background:#1677ff;display:flex;align-items:center;justify-content:center;cursor:pointer;font-size:20px;color:#fff" @click="ali"><svg viewBox="0 0 24 24" width="20" height="20" fill="white"><path d="M12 2L3 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-9-4zm-2 15l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg></div>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { register } from '@/api'
const router = useRouter(), route = useRoute()
const auth = useAuthStore()
const tab = ref(0)
const loading = ref(false)
const remember = ref(true)
const codeSending = ref(false)
const codeCount = ref(60)
const form = reactive({ username:'', password:'', phone:'', code:'' })
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
  // 验证码登录未接入短信服务，不再伪造验证码
  ElMessage.info('验证码登录暂未开放，请使用密码登录')
}

import { ElMessage } from 'element-plus'
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
