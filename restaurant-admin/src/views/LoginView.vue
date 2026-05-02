<template>
  <main class="login-page">
    <form class="login-panel" @submit.prevent="submit">
      <h1>餐厅点餐系统</h1>
      <p>Web 管理端</p>
      <label>
        手机号
        <input v-model="form.phone" placeholder="13800000000" />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" placeholder="123456" />
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
const form = reactive({ phone: '13800000000', password: '123456' })

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const data = await login(form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    router.push('/dashboard')
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
