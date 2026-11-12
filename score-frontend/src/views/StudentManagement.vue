<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { getStudentList } from '@/api/student'
import { useTermStore } from '@/stores/term'
import type { Student } from '@/types'

const termStore = useTermStore()

const loading = ref(false)
const errorMessage = ref('')
const studentList = ref<Student[]>([])
const total = ref(0)
const searchForm = reactive({ className: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10 })

const loadData = async () => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getStudentList({
      className: searchForm.className.trim(),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      academicYear: termStore.academicYear,
      semester: termStore.semester,
    })
    studentList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    studentList.value = []
    total.value = 0
    errorMessage.value = '学生信息加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.className = ''
  pagination.pageNum = 1
  loadData()
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

watch(
  () => termStore.selectedKey,
  () => {
    if (termStore.loaded) {
      pagination.pageNum = 1
      loadData()
    }
  },
)

onMounted(async () => {
  document.title = '学生管理 · 成绩查询平台'
  await termStore.load().catch(() => {})
  loadData()
})
</script>

<template>
  <section class="students-page">
    <header class="page-head">
      <div>
        <h1>学生管理</h1>
        <p>按班级查看已导入的学生信息</p>
      </div>
      <el-button @click="loadData" :loading="loading">刷新</el-button>
    </header>

    <div class="panel filter-bar">
      <div class="filter-field">
        <label for="student-class">班级</label>
        <el-input
          id="student-class"
          v-model="searchForm.className"
          placeholder="输入班级名称"
          clearable
          @keydown.enter="handleSearch"
        />
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div v-if="!errorMessage && !loading" class="summary">共 <strong>{{ total }}</strong> 名学生</div>

    <div v-if="errorMessage" class="panel state-panel">
      <h2>学生信息加载失败</h2>
      <p>{{ errorMessage }}</p>
      <el-button @click="loadData">重试</el-button>
    </div>

    <div v-else class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="studentList"
        empty-text="暂无学生记录"
        style="width: 100%"
      >
        <el-table-column label="姓名" prop="studentName" width="120" fixed="left">
          <template #default="{ row }">
            <span class="name-cell">{{ row.studentName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="班级" prop="className" width="130" />
        <el-table-column label="身份证号" prop="idCard" min-width="190">
          <template #default="{ row }">
            <span class="mono">{{ row.idCard || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="录入日期" width="130">
          <template #default="{ row }">
            {{ row.createTime?.split(' ')[0] || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status" :class="{ inactive: row.delFlag === '1' }">
              <i />
              {{ row.delFlag === '1' ? '停用' : '在读' }}
            </span>
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
  </section>
</template>

<style scoped>
.students-page {
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

.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-field {
  flex: 1;
  min-width: 180px;
}

.filter-field label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.filter-actions {
  display: flex;
  gap: 8px;
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

.mono {
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-success);
  white-space: nowrap;
}

.status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.status.inactive {
  color: var(--color-text-muted);
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

@media (max-width: 1100px) {
  .table-hint {
    display: block;
  }
}

@media (max-width: 767px) {
  .page-head h1 {
    font-size: 23px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 14px;
  }

  .filter-actions :deep(.el-button) {
    flex: 1;
  }

  .table-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
