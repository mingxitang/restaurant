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

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')
const nav = [
  { name: '首页', path: '/dashboard' },
  { name: '身份管理', path: '/users' },
  { name: '菜品管理', path: '/dishes' },
  { name: '分类管理', path: '/categories' },
  { name: '菜品统计', path: '/reports' }
]

function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>
