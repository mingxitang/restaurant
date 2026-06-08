import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import TableEntry from './views/TableEntry.vue'
import MenuView from './views/MenuView.vue'
import MyOrderView from './views/MyOrderView.vue'
import PayView from './views/PayView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: TableEntry, meta: { requiresAuth: true } },
    { path: '/menu', component: MenuView, meta: { requiresAuth: true } },
    { path: '/cart', redirect: '/menu', meta: { requiresAuth: true } },
    { path: '/order', component: MyOrderView, meta: { requiresAuth: true } },
    { path: '/pay', component: PayView, meta: { requiresAuth: true } }
  ]
})

router.beforeEach((to) => {
  if (to.meta?.requiresAuth && !sessionStorage.getItem('token')) {
    return '/login'
  }
})

export default router
