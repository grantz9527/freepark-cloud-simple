import { createRouter, createWebHistory } from 'vue-router'
import { clearAuth, getToken } from '../utils/auth'
import { fetchMe } from '../api/user'
import { useUiModeStore } from '../stores/uiMode'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('../layouts/ClassicLayout.vue'),
      children: [
        {
          path: '',
          name: 'overview',
          component: () => import('../views/OverviewView.vue'),
          meta: { titleKey: 'menu.overview' }
        },
        {
          path: 'lots',
          name: 'lots',
          component: () => import('../views/LotsView.vue'),
          meta: { titleKey: 'menu.lots' }
        },
        {
          path: 'lots/:lotId/map',
          name: 'lot-map',
          component: () => import('../views/LotMapView.vue'),
          props: true,
          meta: { titleKey: 'menu.lotMap' }
        },
        {
          path: 'lots/:lotId/map/edit',
          name: 'lot-map-edit',
          component: () => import('../views/LotMapView.vue'),
          props: true,
          meta: { titleKey: 'menu.lotMapEdit', adminOnly: true }
        },
        {
          path: 'lanes',
          name: 'lanes',
          component: () => import('../views/LanesView.vue'),
          meta: { titleKey: 'menu.lanes' }
        },
        {
          path: 'spaces',
          name: 'spaces',
          component: () => import('../views/SpacesView.vue'),
          meta: { titleKey: 'menu.spaces' }
        },
        {
          path: 'booths',
          name: 'booths',
          component: () => import('../views/BoothsView.vue'),
          meta: { titleKey: 'menu.booths' }
        },
        {
          path: 'internal-vehicles',
          name: 'internal-vehicles',
          component: () => import('../views/InternalVehiclesView.vue'),
          meta: { titleKey: 'menu.internalVehicles' }
        },
        {
          path: 'access/whitelist',
          name: 'whitelist',
          component: () => import('../views/WhitelistView.vue'),
          meta: { titleKey: 'menu.whitelist' }
        },
        {
          path: 'access/blacklist',
          name: 'blacklist',
          component: () => import('../views/BlacklistView.vue'),
          meta: { titleKey: 'menu.blacklist' }
        },
        {
          path: 'access/pattern-allowlist',
          name: 'pattern-allowlist',
          component: () => import('../views/PatternAllowlistView.vue'),
          meta: { titleKey: 'menu.patternAllowlist' }
        },
        {
          path: 'access/judgment',
          name: 'access-judgment',
          component: () => import('../views/AccessJudgmentView.vue'),
          meta: { titleKey: 'menu.accessJudgment' }
        },
        {
          path: 'parking/sessions',
          name: 'parking-sessions',
          component: () => import('../views/ParkingSessionsView.vue'),
          meta: { titleKey: 'menu.parkingSessions' }
        },
        {
          path: 'parking/discount-vehicles',
          name: 'discount-vehicles',
          component: () => import('../views/DiscountVehiclesView.vue'),
          meta: { titleKey: 'menu.discountVehicles' }
        },
        {
          path: 'parking/fee-query',
          name: 'vehicle-fee-query',
          component: () => import('../views/FeeQueryView.vue'),
          meta: { titleKey: 'menu.vehicleFeeQuery' }
        },
        {
          path: 'billing/general-rules',
          name: 'billing-general-rules',
          component: () => import('../views/GeneralChargeRulesView.vue'),
          meta: { titleKey: 'menu.generalBillingRules' }
        },
        {
          path: 'billing/daily-rules',
          name: 'billing-daily-rules',
          component: () => import('../views/DailyChargeRulesView.vue'),
          meta: { titleKey: 'menu.dailyBillingRules' }
        },
        {
          path: 'billing/date-management',
          name: 'billing-date-management',
          component: () => import('../views/DateManagementView.vue'),
          meta: { titleKey: 'menu.dateManagement' }
        },
        {
          path: 'billing/cycle-profiles',
          name: 'billing-cycle-profiles',
          component: () => import('../views/BillingCycleProfilesView.vue'),
          meta: { titleKey: 'menu.billingCycleProfiles' }
        },
        {
          path: 'system/admins',
          name: 'system-admins',
          component: () => import('../views/UserManageView.vue'),
          meta: { titleKey: 'menu.systemAdmins' }
        },
        {
          path: 'system/settings',
          name: 'system-settings',
          component: () => import('../views/SystemSettingsView.vue'),
          meta: { titleKey: 'menu.systemSettings' }
        },
        {
          path: 'system/edge-computing',
          name: 'edge-computing',
          component: () => import('../views/EdgeComputingView.vue'),
          meta: { titleKey: 'menu.edgeComputing' }
        },
        {
          path: 'system/edge-nodes',
          name: 'edge-nodes',
          component: () => import('../views/EdgeNodesView.vue'),
          meta: { titleKey: 'menu.edgeNodes' }
        },
        {
          path: 'system/edge-heartbeat',
          name: 'edge-heartbeat',
          component: () => import('../views/EdgeHeartbeatView.vue'),
          meta: { titleKey: 'menu.edgeHeartbeat' }
        }
      ]
    },
    {
      path: '/ai',
      name: 'ai',
      component: () => import('../layouts/AiLayout.vue'),
      meta: { titleKey: 'ai.title' }
    }
  ]
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 本地未缓存用户信息时（如刷新页面）向 /me 拉取角色，供菜单按角色过滤
  const userStore = useUserStore()
  if (!userStore.user) {
    try {
      const me = await fetchMe()
      userStore.save({ username: me.username, nickname: me.nickname ?? '', role: me.role })
    } catch {
      clearAuth()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  // 编辑地图仅超级管理员可进入，其余回落到只读地图
  if (to.meta.adminOnly && !userStore.isSuperAdmin) {
    return { name: 'lot-map', params: { lotId: to.params.lotId } }
  }

  const uiMode = useUiModeStore()
  if (to.path.startsWith('/ai')) {
    uiMode.setMode('ai')
  } else {
    uiMode.setMode('classic')
  }
  return true
})

export default router
