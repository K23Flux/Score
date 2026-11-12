<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { checkList } from '@/api/score'
import { useTermStore } from '@/stores/term'
import type { ScoreGroupItem } from '@/types'

const termStore = useTermStore()

const loading = ref(false)
const errorMessage = ref('')
const scores = ref<ScoreGroupItem[]>([])

const searchForm = reactive({
  studentName: '',
  examName: '',
})

const examCount = computed(() => new Set(scores.value.map((item) => item.examName)).size)

const formatScore = (value: number | string) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return Number.isInteger(number) ? String(number) : String(Math.round(number * 100) / 100)
}

const loadData = async () => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await checkList({
      studentName: searchForm.studentName.trim(),
      examName: searchForm.examName.trim(),
      academicYear: termStore.academicYear,
      semester: termStore.semester,
    })
    scores.value = res.data || []
  } catch {
    scores.value = []
    errorMessage.value = '成绩加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadData()

const handleReset = () => {
  searchForm.studentName = ''
  searchForm.examName = ''
  loadData()
}

watch(
  () => termStore.selectedKey,
  () => {
    if (termStore.loaded) loadData()
  },
)

onMounted(async () => {
  document.title = '成绩管理 · 成绩查询平台'
  await termStore.load().catch(() => {})
  loadData()
})
</script>

<template>
  <section class="dashboard-page">
    <header class="page-head">
      <div>
        <h1>成绩管理</h1>
        <p>按学生和考试筛选已录入的成绩</p>
      </div>
      <el-button @click="loadData" :loading="loading">刷新</el-button>
    </header>

    <div class="panel filter-bar">
      <div class="filter-field">
        <label for="dashboard-student">学生姓名</label>
        <el-input
          id="dashboard-student"
          v-model="searchForm.studentName"
          placeholder="输入姓名"
          clearable
          @keydown.enter="handleSearch"
        />
      </div>
      <div class="filter-field">
        <label for="dashboard-exam">考试名称</label>
        <el-input
          id="dashboard-exam"
          v-model="searchForm.examName"
          placeholder="输入考试名称"
          clearable
          @keydown.enter="handleSearch"
        />
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div v-if="!errorMessage && !loading" class="summary">
      查询结果：<strong>{{ scores.length }}</strong> 条汇总记录，共
      <strong>{{ examCount }}</strong> 个考试名称。
    </div>

    <div v-if="errorMessage" class="panel state-panel">
      <h2>成绩加载失败</h2>
      <p>{{ errorMessage }}</p>
      <el-button @click="loadData">重试</el-button>
    </div>

    <div v-else class="panel table-panel">
      <el-table
        v-loading="loading"
        :data="scores"
        empty-text="暂无成绩记录"
        style="width: 100%"
      >
        <el-table-column label="姓名" prop="studentName" width="120" fixed="left">
          <template #default="{ row }">
            <span class="name-cell">{{ row.studentName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="班级" prop="className" width="120" />
        <el-table-column label="考试" prop="examName" min-width="140" />
        <el-table-column label="各科成绩" min-width="280">
          <template #default="{ row }">
            <div class="subjects">
              <span v-for="(value, name) in row.subjects" :key="name">
                {{ name }} <b>{{ formatScore(value) }}</b>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="90" align="right">
          <template #default="{ row }">
            <span class="total-score">{{ formatScore(row.totalScore) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="录入日期" width="120">
          <template #default="{ row }">
            {{ row.createTime?.split(' ')[0] || '—' }}
          </template>
        </el-table-column>
      </el-table>
      <p class="table-hint">左右滑动可查看完整表格</p>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
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

.subjects {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.subjects b {
  font-weight: 600;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.total-score {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.table-hint {
  display: none;
  margin: 0;
  padding: 8px 16px 12px;
  font-size: 12px;
  color: var(--color-text-muted);
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
}
</style>
