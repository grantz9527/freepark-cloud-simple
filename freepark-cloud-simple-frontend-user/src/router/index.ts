import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/pay/:payNo',
      name: 'pay-status',
      component: () => import('../views/PaymentStatusView.vue')
    }
  ]
})

export default router
