<template>
  <main class="login-page">
    <form class="login-panel" @submit.prevent="submit">
      <h1>欢迎光临</h1>
      <p>扫码点餐 · 自助下单</p>
      <label>
        手机号
        <input v-model="form.phone" placeholder="请输入手机号" />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </label>
      <button :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      <span class="error" v-if="error">{{ error }}</span>
    </form>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  phone: import.meta.env.VITE_DEMO_PHONE || '',
  password: import.meta.env.VITE_DEMO_PASSWORD || ''
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const data = await login(form)
    sessionStorage.setItem('token', data.token)
    sessionStorage.setItem('user', JSON.stringify(data))
    router.push('/')
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
