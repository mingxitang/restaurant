import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import Layout from './views/Layout.vue'
import DashboardView from './views/DashboardView.vue'
import UsersView from './views/UsersView.vue'
import DishesView from './views/DishesView.vue'
import CategoriesView from './views/CategoriesView.vue'
import ReportsView from './views/ReportsView.vue'
import KitchenView from './views/KitchenView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView, meta: { roles: ['管理员', '服务员', '厨师'] } },
        { path: 'users', component: UsersView, meta: { roles: ['管理员'] } },
        { path: 'dishes', component: DishesView, meta: { roles: ['管理员', '厨师'] } },
        { path: 'categories', component: CategoriesView, meta: { roles: ['管理员', '厨师'] } },
        { path: 'reports', component: ReportsView, meta: { roles: ['管理员', '服务员', '厨师'] } },
        { path: 'kitchen', component: KitchenView, meta: { roles: ['管理员', '厨师'] } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !sessionStorage.getItem('token')) {
    return '/login'
  }
  const user = JSON.parse(sessionStorage.getItem('user') || 'null')
  const roles = to.meta?.roles
  if (roles && !roles.includes(user?.roleName)) {
    return '/dashboard'
  }
})

export default router
