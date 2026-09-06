<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createAdmin,
  fetchAdmins,
  resetAdminPassword,
  updateAdminStatus,
  type UserItem
} from '../api/user'
import { useUserStore } from '../stores/user'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

/** 仅超级管理员可管理管理员账号，其他角色直接给出无权限提示 */
const forbidden = computed(() => !userStore.isSuperAdmin)

const loading = ref(false)
const rows = ref<UserItem[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const isSelf = (row: UserItem) => row.username === userStore.user?.username
const isProtected = (row: UserItem) => row.role === 'SUPER_ADMIN' || isSelf(row)

/**
 * 后端返回的本地挂钟时间文本（如 2026-09-07T03:01:02），
 * 该时间在系统配置时区下解释，因此按纯文本格式化，不经过 new Date 的浏览器本地时区换算。
 */
function formatTime(value?: string): string {
  if (!value) {
    return '-'
  }
  const match = /^(\d{4})-(\d{1,2})-(\d{1,2})[T ](\d{1,2}):(\d{2})/.exec(value)
  if (!match) {
    return value
  }
  const pad = (n: string): string => n.padStart(2, '0')
  return `${match[1]}-${pad(match[2])}-${pad(match[3])} ${pad(match[4])}:${match[5]}`
}

async function loadList() {
  if (forbidden.value) {
    return
  }
  loading.value = true
  try {
    const data = await fetchAdmins({
      keyword: query.keyword.trim() || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('common.requestFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.page = 1
  loadList()
}

function handlePageChange(page: number) {
  query.page = page
  loadList()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  loadList()
}

function onToggleClick(row: UserItem) {
  if (isProtected(row)) {
    ElMessage.warning(
      row.role === 'SUPER_ADMIN'
        ? t('userMgmt.superAdminProtected')
        : t('userMgmt.cannotDisableSelf')
    )
    return
  }
  handleToggleStatus(row)
}

async function handleToggleStatus(row: UserItem) {
  const next = row.status === 1 ? 0 : 1
  const name = row.username
  try {
    await ElMessageBox.confirm(
      next === 0 ? t('userMgmt.disableConfirm', { name }) : t('userMgmt.enableConfirm', { name }),
      t('common.logoutTitle'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    // 用户取消时恢复显示为服务端真实状态
    loadList()
    return
  }
  try {
    await updateAdminStatus(row.id, next)
    ElMessage.success(t('userMgmt.statusSuccess'))
    loadList()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('common.requestFailed'))
  }
}

async function handleResetPassword(row: UserItem) {
  try {
    const { value } = await ElMessageBox.prompt('', t('userMgmt.resetPwd'), {
      inputType: 'password',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputPlaceholder: t('userMgmt.passwordPlaceholder'),
      inputValidator: (val: string) => {
        if (!val || val.length < 6) {
          return t('userMgmt.passwordRule')
        }
        return true
      }
    })
    await ElMessageBox.confirm(
      t('userMgmt.resetPwdConfirm', { name: row.username }),
      t('userMgmt.resetPwd'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
    await resetAdminPassword(row.id, value)
    ElMessage.success(t('userMgmt.resetPwdSuccess'))
  } catch (error) {
    if (error instanceof Error && error.message) {
      ElMessage.error(error.message)
    }
  }
}

/* ---------------- 新增管理员 ---------------- */

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const formRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('userMgmt.dialogUsername'), trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,64}$/, message: t('userMgmt.usernameRule'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('userMgmt.dialogPassword'), trigger: 'blur' },
    { min: 6, message: t('userMgmt.passwordRule'), trigger: 'blur' }
  ]
}))

function openCreateDialog() {
  form.username = ''
  form.password = ''
  form.nickname = ''
  dialogVisible.value = true
}

async function handleCreate() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await createAdmin({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined
    })
    ElMessage.success(t('userMgmt.createSuccess'))
    dialogVisible.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('common.requestFailed'))
  } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <section class="user-mgmt">
    <!-- 无权限提示（普通管理员直接访问本页时展示，而非空表 + 报错） -->
    <div v-if="forbidden" class="no-access">
      <div class="no-access-icon">!</div>
      <h3>{{ t('userMgmt.noAccessTitle') }}</h3>
      <p>{{ t('userMgmt.noAccessDesc') }}</p>
      <el-button type="primary" @click="router.push('/')">{{ t('userMgmt.backToOverview') }}</el-button>
    </div>

    <template v-else>
      <el-card class="panel" shadow="never">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            class="search-input"
            :placeholder="t('userMgmt.searchPlaceholder')"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">{{ t('userMgmt.search') }}</el-button>
          <el-button @click="handleReset">{{ t('userMgmt.reset') }}</el-button>
          <div class="spacer" />
          <el-button type="primary" plain @click="openCreateDialog">
            {{ t('userMgmt.add') }}
          </el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="username" :label="t('userMgmt.username')" min-width="140" />
          <el-table-column prop="nickname" :label="t('userMgmt.nickname')" min-width="140">
            <template #default="{ row }">
              {{ row.nickname || '-' }}
              <span v-if="isSelf(row)" class="self-tag">{{ t('userMgmt.self') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('userMgmt.role')" width="150">
            <template #default="{ row }">
              <el-tag
                :type="row.role === 'SUPER_ADMIN' ? 'warning' : 'primary'"
                effect="light"
                round
              >
                {{ row.role === 'SUPER_ADMIN' ? t('userMgmt.roleSuperAdmin') : t('userMgmt.roleAdmin') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('userMgmt.status')" width="130">
            <template #default="{ row }">
              <el-tooltip
                :disabled="!isProtected(row)"
                :content="
                  row.role === 'SUPER_ADMIN' ? t('userMgmt.superAdminProtected') : t('userMgmt.cannotDisableSelf')
                "
                placement="top"
              >
                <span class="switch-wrap">
                  <el-switch
                    :model-value="row.status === 1"
                    :disabled="isProtected(row)"
                    inline-prompt
                    :active-text="t('userMgmt.enabled')"
                    :inactive-text="t('userMgmt.disabled')"
                    @click="onToggleClick(row)"
                  />
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column :label="t('userMgmt.createdAt')" min-width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('userMgmt.actions')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleResetPassword(row)">
                {{ t('userMgmt.resetPwd') }}
              </el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty :description="t('userMgmt.emptyList')" :image-size="80" />
          </template>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </el-card>

      <el-dialog
        v-model="dialogVisible"
        :title="t('userMgmt.dialogTitle')"
        width="min(460px, 92vw)"
        destroy-on-close
        append-to-body
      >
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
          <el-form-item :label="t('userMgmt.dialogUsername')" prop="username">
            <el-input
              v-model="form.username"
              :placeholder="t('userMgmt.usernamePlaceholder')"
              maxlength="64"
            />
          </el-form-item>
          <el-form-item :label="t('userMgmt.dialogNickname')" prop="nickname">
            <el-input
              v-model="form.nickname"
              :placeholder="t('userMgmt.nicknamePlaceholder')"
              maxlength="64"
            />
          </el-form-item>
          <el-form-item :label="t('userMgmt.dialogPassword')" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="t('userMgmt.passwordPlaceholder')"
              autocomplete="new-password"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="handleCreate">
            {{ t('common.confirm') }}
          </el-button>
        </template>
      </el-dialog>
    </template>
  </section>
</template>

<style scoped>
/* 仅做淡入过渡：不使用带 transform 的动画，避免在元素上残留
   transform（残留 transform 会成为 fixed 弹层的包含块，导致遮罩错位） */
.user-mgmt {
  animation: fade-in 0.35s ease;
}

.no-access {
  max-width: 560px;
  margin: 8vh auto 0;
  padding: 48px 32px;
  text-align: center;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
}

.no-access-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--fp-amber);
  border-radius: 50%;
  background: var(--fp-amber-soft);
}

.no-access h3 {
  margin: 0 0 8px;
  font-family: var(--fp-font-display);
  font-size: 1.1rem;
  color: var(--fp-ink);
}

.no-access p {
  margin: 0 0 22px;
  line-height: 1.65;
  color: var(--fp-muted);
}

.panel {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

.spacer {
  flex: 1;
}

.self-tag {
  margin-left: 6px;
  font-size: 0.72rem;
  color: var(--fp-muted);
  border: 1px solid var(--fp-line);
  border-radius: 6px;
  padding: 0 5px;
}

.switch-wrap {
  display: inline-block;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
