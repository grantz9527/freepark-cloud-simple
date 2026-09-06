<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { classicMenus, findOpenMenuIds, menusForRole, type MenuItem } from '../config/menus'
import { useUserStore } from '../stores/user'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuItems = computed(() =>
  menusForRole(classicMenus, userStore.user?.role as string | undefined)
)
const activePath = computed(() => route.path || '/')
const defaultOpeneds = computed(() => findOpenMenuIds(menuItems.value, activePath.value))

function onSelect(index: string) {
  if (index && index !== route.path) {
    router.push(index)
  }
}

function hasChildren(item: MenuItem) {
  return Boolean(item.children?.length)
}
</script>

<template>
  <el-menu
    class="side-menu"
    :key="defaultOpeneds.join('|')"
    :default-active="activePath"
    :default-openeds="defaultOpeneds"
    background-color="transparent"
    text-color="rgba(232, 239, 236, 0.78)"
    active-text-color="#ffffff"
    @select="onSelect"
  >
    <template v-for="l1 in menuItems" :key="l1.id">
      <el-menu-item v-if="!hasChildren(l1) && l1.path" :index="l1.path">
        <span>{{ t(l1.titleKey) }}</span>
      </el-menu-item>

      <el-sub-menu v-else :index="l1.id">
        <template #title>
          <span>{{ t(l1.titleKey) }}</span>
        </template>

        <template v-for="l2 in l1.children" :key="l2.id">
          <el-menu-item v-if="!hasChildren(l2) && l2.path" :index="l2.path">
            <span>{{ t(l2.titleKey) }}</span>
          </el-menu-item>

          <el-sub-menu v-else :index="l2.id">
            <template #title>
              <span>{{ t(l2.titleKey) }}</span>
            </template>
            <el-menu-item
              v-for="l3 in l2.children"
              :key="l3.id"
              :index="l3.path || l3.id"
              :disabled="!l3.path"
            >
              <span>{{ t(l3.titleKey) }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<style scoped>
.side-menu {
  border-right: none;
  width: 100%;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-bg-color: transparent;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 10px;
  margin-bottom: 2px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(13, 122, 111, 0.35) !important;
}

.side-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  padding-left: 36px !important;
  min-width: 0;
}

.side-menu :deep(.el-sub-menu .el-sub-menu .el-menu-item) {
  padding-left: 48px !important;
}

.side-menu :deep(.el-sub-menu__icon-arrow) {
  color: rgba(232, 239, 236, 0.45);
}
</style>
