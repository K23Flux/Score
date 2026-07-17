<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { checkList } from '@/api/score'
import { ElMessage } from 'element-plus'
import {
  PhMagnifyingGlass,
  PhArrowsClockwise,
  PhStudent,
  PhExam,
  PhChartBar,
  PhFiles,
} from '@phosphor-icons/vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import type { ScoreGroupItem } from '@/types'

gsap.registerPlugin(ScrollTrigger)

const loading = ref(false)
const scores = ref<ScoreGroupItem[]>([])
const heroRef = ref<HTMLElement | null>(null)

const searchForm = reactive({
  studentName: '',
  examName: '',
})

const stats = computed(() => {
  const uniqueStudents = new Set(scores.value.map((s) => s.studentName))
  const uniqueExams = new Set(scores.value.map((s) => s.examName))
  const totalScores = scores.value.reduce((sum, s) => sum + s.totalScore, 0)
  const avgScore = scores.value.length > 0
    ? (totalScores / scores.value.length).toFixed(1)
    : '0'
  return { students: uniqueStudents.size, exams: uniqueExams.size, avgScore, records: scores.value.length }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await checkList(searchForm)
    scores.value = res.data || []
  } catch { ElMessage.error('DATA LOAD FAILED') }
  finally { loading.value = false }
}

const handleSearch = () => loadData()
const handleReset = () => { searchForm.studentName = ''; searchForm.examName = ''; loadData() }

const getScoreColor = (totalScore: number, subjects: Record<string, number>) => {
  const count = Object.keys(subjects).length || 1
  const avg = totalScore / count
  if (avg >= 90) return 'var(--emerald)'
  if (avg >= 80) return 'var(--amber)'
  if (avg >= 60) return 'var(--orange)'
  return 'var(--red)'
}

// Status dot indicator
const getStatusDot = (totalScore: number, subjects: Record<string, number>) => {
  const count = Object.keys(subjects).length || 1
  const avg = totalScore / count
  if (avg >= 80) return 'emerald'
  if (avg >= 60) return 'amber'
  return 'red'
}

let ctx: { revert: () => void } | null = null

onMounted(() => {
  document.title = 'ScoreCheck — Dashboard'
  loadData()

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReduced) return

  ctx = gsap.context(() => {
    // Hero fade in
    gsap.from('.hero-section', {
      opacity: 0,
      y: 20,
      duration: 0.6,
      ease: 'power2.out',
      delay: 0.1,
    })

    // Stats counters stagger
    gsap.from('.stat-value', {
      opacity: 0,
      scale: 0.8,
      stagger: 0.08,
      duration: 0.4,
      ease: 'power2.out',
      delay: 0.3,
    })

    // Table fade in
    gsap.from('.table-section', {
      opacity: 0,
      y: 16,
      duration: 0.5,
      ease: 'power2.out',
      delay: 0.5,
    })
  })
})

onBeforeUnmount(() => { ctx?.revert() })
</script>

<template>
  <div class="scorecheck-dashboard">
    <!-- Hero Section -->
    <div ref="heroRef" class="hero-section">
      <div class="hero-left">
        <div class="hero-title">
          <span class="hero-prefix">[</span>
          <span class="hero-heading">SCORECHECK</span>
          <span class="hero-suffix">]</span>
        </div>
        <div class="hero-sub">CLASS PERFORMANCE OVERVIEW</div>
      </div>
      <el-button class="hero-refresh" @click="loadData" :loading="loading">
        <template #icon><PhArrowsClockwise size="14" weight="bold" /></template>
        REFRESH
      </el-button>
    </div>

    <!-- Stats Row -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">STUDENTS</div>
        <div class="stat-value">{{ stats.students }}</div>
        <div class="stat-meta">enrolled</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">EXAMS</div>
        <div class="stat-value">{{ stats.exams }}</div>
        <div class="stat-meta">sessions</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">AVG SCORE</div>
        <div class="stat-value">{{ stats.avgScore }}</div>
        <div class="stat-meta">class average</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">RECORDS</div>
        <div class="stat-value">{{ stats.records }}</div>
        <div class="stat-meta">total entries</div>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.studentName"
        placeholder="> STUDENT NAME..."
        clearable
        class="search-input"
        :prefix-icon="PhMagnifyingGlass"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchForm.examName"
        placeholder="> EXAM NAME..."
        clearable
        class="search-input"
        :prefix-icon="PhMagnifyingGlass"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch" :loading="loading">SEARCH</el-button>
      <el-button text @click="handleReset">RESET</el-button>
    </div>

    <!-- Data Table -->
    <div class="table-section">
      <el-table
        v-loading="loading"
        :data="scores"
        style="width: 100%"
        empty-text="NO DATA RECORDS FOUND"
        :row-class-name="() => 'terminal-row'"
      >
        <el-table-column type="index" width="48" align="center">
          <template #header>
            <span class="col-label">#</span>
          </template>
          <template #default="{ $index }">
            <span class="col-index">{{ $index + 1 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="STUDENT" min-width="160">
          <template #header>
            <span class="col-label">NAME</span>
          </template>
          <template #default="{ row }">
            <div class="student-cell">
              <span
                class="status-dot"
                :class="getStatusDot(row.totalScore, row.subjects)"
              />
              <span class="stu-name">{{ row.studentName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="CLASS" width="130">
          <template #header>
            <span class="col-label">CLASS</span>
          </template>
          <template #default="{ row }">
            <span class="class-text">{{ row.className || '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="EXAM" min-width="180">
          <template #header>
            <span class="col-label">EXAM</span>
          </template>
          <template #default="{ row }">
            <span class="exam-text">{{ row.examName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="SUBJECTS" min-width="280">
          <template #header>
            <span class="col-label">SUBJECTS</span>
          </template>
          <template #default="{ row }">
            <div class="subject-strip">
              <span
                v-for="(score, subject) in row.subjects"
                :key="subject"
                class="subject-chip"
                :class="{ low: score < 60, high: score >= 90 }"
              >
                <span class="subj-name">{{ subject }}</span>
                <span class="subj-score">{{ score }}</span>
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="TOTAL" width="100" align="center">
          <template #header>
            <span class="col-label">TOTAL</span>
          </template>
          <template #default="{ row }">
            <span class="total-score" :style="{ color: getScoreColor(row.totalScore, row.subjects) }">
              {{ row.totalScore }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="DATE" width="130">
          <template #header>
            <span class="col-label">DATE</span>
          </template>
          <template #default="{ row }">
            <span class="date-text">{{ row.createTime?.split(' ')[0] || '--' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.scorecheck-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* --- Hero --- */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hero-title {
  display: flex;
  align-items: center;
  gap: 0;
}

.hero-prefix, .hero-suffix {
  font-size: 28px;
  font-weight: 300;
  color: var(--amber);
  line-height: 1;
}

.hero-heading {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.15em;
}

.hero-sub {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-refresh {
  height: 32px !important;
  padding: 0 16px !important;
  border-radius: 0 !important;
  font-family: 'IBM Plex Mono', monospace !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  letter-spacing: 0.06em !important;
}

/* --- Stats Grid --- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--border);
  border: 1px solid var(--border);
}

.stat-card {
  background: var(--bg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-meta {
  font-size: 10px;
  color: var(--text-muted);
  letter-spacing: 0.04em;
}

/* --- Search Bar --- */
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
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

/* --- Table Section --- */
.table-section {
  background: var(--bg);
  border: 1px solid var(--border);
  overflow: hidden;
}

/* Column labels */
.col-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.col-index {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

/* Student cell */
.student-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  flex-shrink: 0;
}

.status-dot.emerald {
  background: var(--emerald);
  animation: breathe 3s ease-in-out infinite;
}

.status-dot.amber {
  background: var(--amber);
  animation: breathe 3s ease-in-out infinite;
}

.status-dot.red {
  background: var(--red);
  animation: breathe 3s ease-in-out infinite;
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

.exam-text {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 500;
}

/* Subject strip */
.subject-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.subject-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  padding: 2px 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  font-size: 11px;
  transition: all var(--fast) var(--ease-out);
}

.subject-chip:hover {
  border-color: var(--border-strong);
}

.subject-chip.low {
  background: var(--red-bg);
  border-color: rgba(248, 81, 73, 0.2);
  color: var(--red);
}

.subject-chip.high {
  background: var(--emerald-bg);
  border-color: rgba(63, 185, 80, 0.2);
  color: var(--emerald);
}

.subj-name {
  color: var(--text-muted);
  font-weight: 400;
}

.subj-score {
  font-weight: 700;
  color: var(--text-primary);
}

.subject-chip.low .subj-score { color: var(--red); }
.subject-chip.high .subj-score { color: var(--emerald); }

/* Total score */
.total-score {
  font-size: 15px;
  font-weight: 700;
}

/* Date */
.date-text {
  font-size: 12px;
  color: var(--text-muted);
}

/* --- Alternating row colors --- */
.el-table .el-table__row:nth-child(even) {
  background-color: var(--bg-deep) !important;
}

.el-table .el-table__row:hover {
  background-color: var(--surface) !important;
}

/* --- Responsive --- */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .hero-heading {
    font-size: 20px;
    letter-spacing: 0.08em;
  }

  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .subject-strip {
    gap: 3px;
  }
}
</style>
