import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Transactions from '../views/Transactions.vue'
import Alerts from '../views/Alerts.vue'
import AlertHistory from '../views/AlertHistory.vue'
import Rules from '../views/Rules.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: Dashboard,
    },
    {
      path: '/transactions',
      name: 'Transactions',
      component: Transactions,
    },
    {
      path: '/alerts',
      name: 'Alerts',
      component: Alerts,
    },
    {
      path: '/alert-history',
      name: 'AlertHistory',
      component: AlertHistory,
    },
    {
      path: '/rules',
      name: 'Rules',
      component: Rules,
    },
  ],
})

export default router
