/**
 * 传统模式侧栏菜单：支持最多两级（目录 / 页面）。
 * - 有 children：目录节点，不直接跳转
 * - 有 path：叶子页面，对应路由
 */
export interface MenuItem {
  id: string
  /** i18n key，如 menu.lots */
  titleKey: string
  path?: string
  children?: MenuItem[]
  /** 可见角色白名单；缺省表示所有角色可见 */
  roles?: string[]
}

export const classicMenus: MenuItem[] = [
  {
    id: 'overview',
    titleKey: 'menu.overview',
    path: '/'
  },
  {
    id: 'lotMgmt',
    titleKey: 'menu.sectionLot',
    children: [
      {
        id: 'lots',
        titleKey: 'menu.lots',
        path: '/lots'
      },
      {
        id: 'lanes',
        titleKey: 'menu.lanes',
        path: '/lanes'
      },
      {
        id: 'spaces',
        titleKey: 'menu.spaces',
        path: '/spaces'
      },
      {
        id: 'booths',
        titleKey: 'menu.booths',
        path: '/booths'
      },
      {
        id: 'internal-vehicles',
        titleKey: 'menu.internalVehicles',
        path: '/internal-vehicles'
      }
    ]
  },
  {
    id: 'accessMgmt',
    titleKey: 'menu.sectionAccess',
    children: [
      {
        id: 'whitelist',
        titleKey: 'menu.whitelist',
        path: '/access/whitelist'
      },
      {
        id: 'blacklist',
        titleKey: 'menu.blacklist',
        path: '/access/blacklist'
      },
      {
        id: 'pattern-allowlist',
        titleKey: 'menu.patternAllowlist',
        path: '/access/pattern-allowlist'
      },
      {
        id: 'access-judgment',
        titleKey: 'menu.accessJudgment',
        path: '/access/judgment'
      }
    ]
  },
  {
    id: 'parkingMgmt',
    titleKey: 'menu.sectionParking',
    children: [
      {
        id: 'parking-sessions',
        titleKey: 'menu.parkingSessions',
        path: '/parking/sessions'
      },
      {
        id: 'parking-orders',
        titleKey: 'menu.parkingOrders',
        path: '/parking/orders'
      },
      {
        id: 'discount-vehicles',
        titleKey: 'menu.discountVehicles',
        path: '/parking/discount-vehicles'
      },
      {
        id: 'vehicle-fee-query',
        titleKey: 'menu.vehicleFeeQuery',
        path: '/parking/fee-query'
      }
    ]
  },
  {
    id: 'billingMgmt',
    titleKey: 'menu.billingRules',
    children: [
      {
        id: 'billing-general-rules',
        titleKey: 'menu.generalBillingRules',
        path: '/billing/general-rules'
      },
      {
        id: 'billing-daily-rules',
        titleKey: 'menu.dailyBillingRules',
        path: '/billing/daily-rules'
      },
      {
        id: 'billing-date-management',
        titleKey: 'menu.dateManagement',
        path: '/billing/date-management'
      },
      {
        id: 'billing-cycle-profiles',
        titleKey: 'menu.billingCycleProfiles',
        path: '/billing/cycle-profiles'
      }
    ]
  },
  {
    id: 'wechat',
    titleKey: 'menu.wechat',
    roles: ['SUPER_ADMIN'],
    children: [
      {
        id: 'wechat-config',
        titleKey: 'menu.wechatConfig',
        path: '/wechat/config'
      }
    ]
  },
  {
    id: 'alipay',
    titleKey: 'menu.alipay',
    roles: ['SUPER_ADMIN'],
    children: [
      {
        id: 'alipay-config',
        titleKey: 'menu.alipayConfig',
        path: '/alipay/config'
      }
    ]
  },
  {
    id: 'system',
    titleKey: 'menu.system',
    roles: ['SUPER_ADMIN'],
    children: [
      {
        id: 'system-admins',
        titleKey: 'menu.systemAdmins',
        path: '/system/admins'
      },
      {
        id: 'system-settings',
        titleKey: 'menu.systemSettings',
        path: '/system/settings'
      },
      {
        id: 'edge-computing',
        titleKey: 'menu.edgeComputing',
        path: '/system/edge-computing'
      },
      {
        id: 'edge-nodes',
        titleKey: 'menu.edgeNodes',
        path: '/system/edge-nodes'
      },
      {
        id: 'edge-heartbeat',
        titleKey: 'menu.edgeHeartbeat',
        path: '/system/edge-heartbeat'
      }
    ]
  }
]

/**
 * 按角色裁剪菜单树：保留无角色要求或角色命中的节点，
 * 目录节点所有子项都被过滤掉时整组隐藏。
 */
export function menusForRole(items: MenuItem[], role?: string): MenuItem[] {
  const prune = (nodes: MenuItem[]): MenuItem[] => {
    const kept: MenuItem[] = []
    for (const node of nodes) {
      if (node.roles && !(role && node.roles.includes(role))) {
        continue
      }
      const copy: MenuItem = { ...node }
      if (node.children?.length) {
        const children = prune(node.children)
        if (!children.length) {
          continue
        }
        copy.children = children
      }
      kept.push(copy)
    }
    return kept
  }
  return prune(items)
}

/** 收集所有叶子 path，便于激活态与默认展开计算 */
export function collectLeafPaths(items: MenuItem[]): string[] {
  const paths: string[] = []
  const walk = (nodes: MenuItem[]) => {
    for (const node of nodes) {
      if (node.children?.length) {
        walk(node.children)
      } else if (node.path) {
        paths.push(node.path)
      }
    }
  }
  walk(items)
  return paths
}

/** 根据当前 path 找出需要展开的父级 id */
export function findOpenMenuIds(items: MenuItem[], currentPath: string): string[] {
  const open: string[] = []

  const walk = (nodes: MenuItem[], parents: string[]): boolean => {
    for (const node of nodes) {
      const chain = [...parents, node.id]
      if (node.path === currentPath) {
        open.push(...parents)
        return true
      }
      if (node.children?.length && walk(node.children, chain)) {
        return true
      }
    }
    return false
  }

  walk(items, [])
  return open
}
