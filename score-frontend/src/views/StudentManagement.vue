<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { getStudentList } from '@/api/student'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PhMagnifyingGlass, PhArrowsClockwise, PhTrash } from '@phosphor-icons/vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import type { Student } from '@/types'

gsap.registerPlugin(ScrollTrigger)

const loading = ref(false)
const studentList = ref<Student[]>([])
const total = ref(0)
const searchForm = reactive({ className: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStudentList({ className: searchForm.className, pageNum: pagination.pageNum, pageSize: pagination.pageSize })
    studentList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('DATA LOAD FAILED') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.pageNum = 1; loadData() }
const handleReset = () => { searchForm.className = ''; pagination.pageNum = 1; loadData() }
const handleDelete = async (row: Student) => {
  try {
    await ElMessageBox.confirm(`DELETE RECORD: ${row.studentName}?`, 'CONFIRM ACTION', {
      confirmButtonText: 'DELETE',
      cancelButtonText: 'CANCEL',
      type: 'warning',
    })
    ElMessage.warning('INTERFACE NOT IMPLEMENTED')
  } catch {}
}
const handlePageChange = (page: number) => { pagination.pageNum = page; loadData() }
const handleSizeChange = (size: number) => { pagination.pageSize = size; pagination.pageNum = 1; loadData() }

let ctx: { revert: () => void } | null = null

onMounted(() => {
  document.title = 'ScoreCheck — Students'
  loadData()

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReduced) return

  ctx = gsap.context(() => {
    gsap.from('.terminal-table', {
      opacity: 0,
      y: 12,
      duration: 0.4,
      ease: 'power2.out',
      delay: 0.2,
    })
  })
})

onBeforeUnmount(() => { ctx?.revert() })
</script>

<template>
  <div class="scorecheck-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <span class="header-bracket">[</span>
        <h1 class="header-title">STUDENT MANAGEMENT</h1>
        <span class="header-bracket">]</span>
      </div>
      <div class="header-meta">
        <span class="meta-dot" />
        <span class="meta-text">{{ total }} RECORDS</span>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.className"
        placeholder="> FILTER BY CLASS..."
        clearable
        class="search-input"
        :prefix-icon="PhMagnifyingGlass"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch" :loading="loading">SEARCH</el-button>
      <el-button @click="handleReset">RESET</el-button>
      <el-button circle :icon="PhArrowsClockwise" @click="loadData" />
    </div>

    <!-- Terminal Table -->
    <div class="terminal-table">
      <el-table
        v-loading="loading"
        :data="studentList"
        style="width: 100%"
        empty-text="NO STUDENT RECORDS FOUND"
      >
        <el-table-column type="index" width="48" align="center">
          <template #header>
            <span class="col-label">#</span>
          </template>
        </el-table-column>

        <el-table-column label="NAME" min-width="180">
          <template #header>
            <span class="col-label">NAME</span>
          </template>
          <template #default="{ row }">
            <div class="stu-cell">
              <span class="stu-avatar">{{ row.studentName?.charAt(0) || '?' }}</span>
              <span class="stu-name">{{ row.studentName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="CLASS" width="140">
          <template #header>
            <span class="col-label">CLASS</span>
          </template>
          <template #default="{ row }">
            <span class="class-text">{{ row.className || '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="ID CARD" width="200">
          <template #header>
            <span class="col-label">ID</span>
          </template>
          <template #default="{ row }">
            <span class="id-text">{{ row.idCard || '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="REGISTERED" width="170">
          <template #header>
            <span class="col-label">DATE</span>
          </template>
          <template #default="{ row }">
            <span class="date-text">{{ row.createTime || '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="STATUS" width="90" align="center">
          <template #header>
            <span class="col-label">STS</span>
          </template>
          <template #default="{ row }">
            <span class="status-pill" :class="row.delFlag === '1' ? 'deleted' : 'active'">
              {{ row.delFlag === '1' ? 'DEL' : 'OK' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="" width="56" align="center">
          <template #default="{ row }">
            <button class="del-btn" @click="handleDelete(row)">
              <PhTrash size="13" weight="bold" />
            </button>
          </template>
        </el-table-column>
      </el-table>

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
    </div>
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

/* --- Search Bar --- */
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--surface);
  border: 1px solid var(--border);
}

.search-input {
  width: 220px;
}

.search-input :deep(.el-input__wrapper) {
  background: var(--bg-deep);
  border-radius: 0;
}

/* --- Terminal Table --- */
.terminal-table {
  background: var(--bg);
  border: 1px solid var(--border);
  overflow: hidden;
}

.col-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

/* Student cell */
.stu-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stu-avatar {
  width: 30px;
  height: 30px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--amber);
  flex-shrink: 0;
}

.stu-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.class-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.id-text {
  font-size: 12px;
  color: var(--text-muted);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.03em;
}

.date-text {
  font-size: 12px;
  color: var(--text-muted);
}

/* Status pill */
.status-pill {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  letter-spacing: 0.06em;
}

.status-pill.active {
  color: var(--emerald);
}

.status-pill.deleted {
  color: var(--red);
}

/* Delete button */
.del-btn {
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

.del-btn:hover {
  border-color: var(--red);
  color: var(--red);
  background: var(--red-bg);
}

/* Alternating rows */
.el-table .el-table__row:nth-child(even) {
  background-color: var(--bg-deep) !important;
}

.el-table .el-table__row:hover {
  background-color: var(--surface) !important;
}

/* Pagination */
.pagination-wrap {
  padding: 10px 0 0;
  display: flex;
  justify-content: flex-end;
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .header-title {
    font-size: 18px;
    letter-spacing: 0.06em;
  }

  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}
</style>
