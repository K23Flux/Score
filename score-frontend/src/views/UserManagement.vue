<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createUser, deleteUser, getUserList, updateUser } from '@/api/user'
import type { AdminUser } from '@/types'

const loading = ref(false)
const errorMessage = ref('')
const userList = ref<AdminUser[]>([])
const total = ref(0)
const pagination = reactive({ pageNum: 1, pageSize: 10 })

const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<AdminUser>({ userName: '', password: '', className: '' })

const rules = computed<FormRules>(() => ({
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: isEdit.value
    ? []
    : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
}))

const loadData = async () => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getUserList(pagination.pageNum, pagination.pageSize)
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    userList.value = []
    total.value = 0
    errorMessage.value = '账户列表加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formData.userName = ''
  formData.password = ''
  formData.className = ''
  nextTick(() => formRef.value?.clearValidate())
}

const openAddDialog = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: AdminUser) => {
  isEdit.value = true
  formData.userName = row.userName
  formData.password = ''
  formData.className = row.className
  nextTick(() => formRef.value?.clearValidate())
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (submitting.value) return
  formData.userName = formData.userName.trim()
  formData.className = formData.className.trim()

  formRef.value?.clearValidate()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser({ userName: formData.userName, className: formData.className })
      ElMessage.success('账户已更新')
    } else {
      await createUser({
        userName: formData.userName,
        password: formData.password,
        className: formData.className,
      })
      ElMessage.success('账户已创建')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 请求错误由统一拦截器提示
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: AdminUser) => {
  try {
    await ElMessageBox.confirm(
      `确定删除账户“${row.userName}”吗？此操作不可恢复。`,
      '删除账户',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true,
      },
    )
    await deleteUser(row.userName)
    ElMessage.success('账户已删除')
    loadData()
  } catch {
    // 取消或请求错误
  }
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

onMounted(() => {
  document.title = '用户管理 · 成绩查询平台'
  loadData()
})
</script>

<template>
  <section class="users-page">
    <header class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>管理教师账户及其负责班级</p>
      </div>
      <el-button type="primary" @click="openAddDialog">新增账户</el-button>
    </header>

    <div v-if="!errorMessage && !loading" class="summary">共 <strong>{{ total }}</strong> 个账户</div>

    <div v-if="errorMessage" class="panel state-panel">
      <h2>账户列表加载失败</h2>
      <p>{{ errorMessage }}</p>
      <el-button @click="loadData">重试</el-button>
    </div>

    <div v-else class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="userList"
        empty-text="暂无账户记录"
        style="width: 100%"
      >
        <el-table-column label="用户名" prop="userName" width="160" fixed="left">
          <template #default="{ row }">
            <span class="name-cell">{{ row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="班级" prop="className" min-width="150" />
        <el-table-column label="创建时间" width="140">
          <template #default="{ row }">
            {{ row.createTime?.split(' ')[0] || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <p class="table-hint">左右滑动可查看完整表格</p>

      <div class="table-foot">
        <span>第 {{ pagination.pageNum }} 页</span>
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑账户' : '新增账户'"
      width="min(440px, calc(100vw - 32px))"
      :close-on-click-modal="false"
      align-center
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="formData.userName" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item label="班级" prop="className">
          <el-input v-model="formData.className" placeholder="如：高一(2)班" />
        </el-form-item>
        <p v-if="isEdit" class="dialog-note">编辑账户仅可修改班级。</p>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.users-page {
  max-width: 1400px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.page-head h1 {
  margin: 0 0 4px;
  font-size: 26px;
  line-height: 1.25;
  font-weight: 600;
  color: var(--color-text);
}

.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
}

.summary {
  padding: 0 2px 12px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.summary strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.table-panel {
  overflow: hidden;
}

.name-cell {
  font-weight: 600;
}

.table-hint {
  display: none;
  margin: 0;
  padding: 8px 16px 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--color-text-muted);
  flex-wrap: wrap;
}

.state-panel {
  padding: 48px 24px;
  text-align: center;
}

.state-panel h2 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-danger);
}

.state-panel p {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.dialog-note {
  margin: -4px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 1100px) {
  .table-hint {
    display: block;
  }
}

@media (max-width: 767px) {
  .page-head h1 {
    font-size: 23px;
  }

  .table-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
