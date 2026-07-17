<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  PhPlus,
  PhPencilSimple,
  PhTrash,
  PhArrowsClockwise,
  PhUserPlus,
  PhShield,
} from '@phosphor-icons/vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import type { AdminUser } from '@/types'

gsap.registerPlugin(ScrollTrigger)

const loading = ref(false)
const userList = ref<AdminUser[]>([])
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref()
const formData = reactive<AdminUser>({ userName: '', password: '', className: '' })
const rules = {
  userName: [{ required: true, message: 'REQUIRED', trigger: 'blur' }],
  password: [{ required: true, message: 'REQUIRED', trigger: 'blur' }],
  className: [{ required: true, message: 'REQUIRED', trigger: 'blur' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList(pagination.pageNum, pagination.pageSize)
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('DATA LOAD FAILED') }
  finally { loading.value = false }
}

const openAddDialog = () => {
  isEdit.value = false
  formData.userName = ''
  formData.password = ''
  formData.className = ''
  dialogVisible.value = true
}

const openEditDialog = (row: AdminUser) => {
  isEdit.value = true
  formData.userName = row.userName
  formData.password = ''
  formData.className = row.className
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateUser(formData)
        ElMessage.success('RECORD UPDATED')
      } else {
        await createUser(formData)
        ElMessage.success('USER CREATED')
      }
      dialogVisible.value = false
      loadData()
    } catch {}
    finally { submitting.value = false }
  })
}

const handleDelete = async (row: AdminUser) => {
  try {
    await ElMessageBox.confirm(
      `CONFIRM DELETE:\n${row.userName}\n\nTHIS ACTION CANNOT BE UNDONE.`,
      'DELETE USER',
      {
        confirmButtonText: 'DELETE',
        cancelButtonText: 'CANCEL',
        type: 'warning',
        distinguishCancelAndClose: true,
      }
    )
    await deleteUser(row.userName)
    ElMessage.success('USER REMOVED')
    loadData()
  } catch {}
}

const handlePageChange = (page: number) => { pagination.pageNum = page; loadData() }
const handleSizeChange = (size: number) => { pagination.pageSize = size; pagination.pageNum = 1; loadData() }

let ctx: { revert: () => void } | null = null

onMounted(() => {
  document.title = 'ScoreCheck — Users'
  loadData()

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReduced) return

  ctx = gsap.context(() => {
    gsap.from('.user-card', {
      opacity: 0,
      y: 8,
      stagger: 0.04,
      duration: 0.3,
      ease: 'power2.out',
      delay: 0.15,
    })
  })
})

onBeforeUnmount(() => { ctx?.revert() })
</script>

<template>
  <div class="scorecheck-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <span class="header-bracket">[</span>
        <h1 class="header-title">USER MANAGEMENT</h1>
        <span class="header-bracket">]</span>
      </div>
      <div class="header-meta">
        <span class="meta-dot" />
        <span class="meta-text">{{ total }} ACCOUNTS</span>
      </div>
      <button class="add-btn" @click="openAddDialog">
        <PhUserPlus size="14" weight="bold" />
        NEW USER
      </button>
    </div>

    <!-- User Cards -->
    <div class="user-list">
      <div
        v-for="(user, index) in userList"
        :key="user.userName"
        class="user-card"
        :class="{ even: index % 2 === 1 }"
      >
        <div class="user-card-left">
          <div class="user-avatar">
            {{ user.userName?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <div class="user-info">
            <span class="user-name">{{ user.userName }}</span>
            <span class="user-class">CLASS: {{ user.className || '--' }}</span>
          </div>
        </div>
        <div class="user-card-right">
          <span class="user-date">{{ user.createTime?.split(' ')[0] || '--' }}</span>
          <div class="user-actions">
            <button class="act edit" @click="openEditDialog(user)">
              <PhPencilSimple size="12" weight="bold" />
            </button>
            <button class="act danger" @click="handleDelete(user)">
              <PhTrash size="12" weight="bold" />
            </button>
          </div>
        </div>
      </div>

      <div v-if="!loading && userList.length === 0" class="empty-state">
        <span class="empty-icon">[ ]</span>
        <p class="empty-text">NO USER RECORDS FOUND</p>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Terminal Modal -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'EDIT USER' : 'NEW USER'"
      width="440px"
      :close-on-click-modal="false"
      align-center
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" class="modal-form">
        <el-form-item label="USERNAME" prop="userName">
          <el-input v-model="formData.userName" placeholder="admin" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="PASSWORD" :prop="isEdit ? '' : 'password'">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isEdit ? 'LEAVE BLANK TO KEEP' : 'ENTER PASSWORD'"
          />
        </el-form-item>
        <el-form-item label="CLASS" prop="className">
          <el-input v-model="formData.className" placeholder="2026 Grade Class 1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">CANCEL</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? 'UPDATE' : 'CREATE' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.scorecheck-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* --- Header --- */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0;
}

.header-bracket {
  font-size: 22px;
  font-weight: 300;
  color: var(--amber);
  line-height: 1.2;
}

.header-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.1em;
  line-height: 1.2;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding-top: 6px;
}

.meta-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--emerald);
  animation: breathe 3s ease-in-out infinite;
}

.meta-text {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.08em;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  flex-shrink: 0;
  padding-top: 10px;
}

.add-btn:hover {
  border-color: var(--amber);
  color: var(--amber);
  background: var(--amber-bg);
}

/* --- User List --- */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--border);
  border: 1px solid var(--border);
}

.user-card {
  background: var(--bg);
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  transition: background var(--fast) var(--ease-out);
}

.user-card.even {
  background: var(--bg-deep);
}

.user-card:hover {
  background: var(--surface);
}

.user-card-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--amber);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-class {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.03em;
}

.user-card-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.user-date {
  font-size: 11px;
  color: var(--text-muted);
}

.user-actions {
  display: flex;
  gap: 4px;
}

.act {
  width: 26px;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.act.edit:hover {
  border-color: var(--blue);
  color: var(--blue);
  background: var(--blue-bg);
}

.act.danger:hover {
  border-color: var(--red);
  color: var(--red);
  background: var(--red-bg);
}

/* --- Empty State --- */
.empty-state {
  padding: 48px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-icon {
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 0.1em;
}

.empty-text {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.06em;
}

/* --- Pagination --- */
.pagination-wrap {
  padding-top: 4px;
}

/* --- Modal Form --- */
.modal-form :deep(.el-form-item__label) {
  font-size: 10px !important;
  font-weight: 600 !important;
  letter-spacing: 0.08em !important;
  color: var(--text-muted) !important;
}

.modal-form :deep(.el-input__wrapper) {
  background: var(--bg-deep);
  border-radius: 0;
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .user-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .user-card-right {
    width: 100%;
    justify-content: space-between;
  }

  .page-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
