<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">餐厅点餐</div>
      <nav>
        <RouterLink v-for="item in nav" :key="item.path" :to="item.path">{{ item.name }}</RouterLink>
      </nav>
    </aside>
    <section class="main">
      <header class="topbar">
        <strong>{{ user?.username || '管理员' }}</strong>
        <button class="ghost" @click="logout">退出</button>
      </header>
      <RouterView />
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { logout as logoutApi } from '../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')
const nav = [
  { name: '首页', path: '/dashboard', roles: ['管理员', '服务员', '厨师'] },
  { name: '身份管理', path: '/users', roles: ['管理员'] },
  { name: '菜品管理', path: '/dishes', roles: ['管理员', '厨师'] },
  { name: '分类管理', path: '/categories', roles: ['管理员', '厨师'] },
  { name: '菜品统计', path: '/reports', roles: ['管理员', '服务员', '厨师'] },
  { name: '厨房看板', path: '/kitchen', roles: ['管理员', '厨师'] }
].filter(item => item.roles.includes(user?.roleName))

async function logout() {
  try { await logoutApi() } catch (_) { /* 请求失败也不阻塞退出 */ }
  localStorage.clear()
  router.push('/login')
}
</script>
