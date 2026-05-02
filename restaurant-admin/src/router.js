import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import Layout from './views/Layout.vue'
import DashboardView from './views/DashboardView.vue'
import UsersView from './views/UsersView.vue'
import DishesView from './views/DishesView.vue'
import CategoriesView from './views/CategoriesView.vue'
import ReportsView from './views/ReportsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: DashboardView },
        { path: 'users', component: UsersView },
        { path: 'dishes', component: DishesView },
        { path: 'categories', component: CategoriesView },
        { path: 'reports', component: ReportsView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) {
    return '/login'
  }
})

export default router
