<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { studentQuery } from '@/api/score'
import { ElMessage } from 'element-plus'
import { PhArrowLeft, PhMagnifyingGlass, PhStudent, PhTrophy, PhCheckCircle } from '@phosphor-icons/vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import type { Score } from '@/types'

gsap.registerPlugin(ScrollTrigger)

const router = useRouter()
const loading = ref(false)
const hasSearched = ref(false)
const scores = ref<Score[]>([])

const formData = reactive({ studentName: '', idCard: '' })
const rules = {
  studentName: [{ required: true, message: 'REQUIRED', trigger: 'blur' }],
  idCard: [{ required: true, message: 'INVALID FORMAT', trigger: 'blur' }, { min: 15, max: 18, message: 'INVALID ID LENGTH', trigger: 'blur' }],
}
const formRef = ref()

const totalScore = computed(() => scores.value.reduce((sum, s) => sum + Number(s.score), 0))
const avgScore = computed(() => scores.value.length === 0 ? '0' : (totalScore.value / scores.value.length).toFixed(1))

const groupedScores = computed(() => {
  const map = new Map<string, Score[]>()
  scores.value.forEach((s) => { const list = map.get(s.examName) || []; list.push(s); map.set(s.examName, list) })
  return Array.from(map.entries())
})

const getColor = (s: number) => s >= 90 ? 'var(--emerald)' : s >= 80 ? 'var(--amber)' : s >= 60 ? 'var(--orange)' : 'var(--red)'
const getGrade = (s: number) => s >= 90 ? 'EXCELLENT' : s >= 80 ? 'GOOD' : s >= 60 ? 'PASS' : 'FAIL'

const handleSearch = async () => {
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await studentQuery(formData)
      scores.value = res.data?.score || []
      hasSearched.value = true
      if (scores.value.length === 0) ElMessage.warning('NO RECORDS FOUND')
    } finally { loading.value = false }
  })
}

const goBack = () => router.push('/login')
const resetSearch = () => { hasSearched.value = false; scores.value = []; formData.studentName = ''; formData.idCard = '' }

let ctx: { revert: () => void } | null = null

onMounted(() => {
  document.title = 'ScoreCheck — Query'

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReduced) return

  ctx = gsap.context(() => {
    gsap.from('.search-panel', {
      opacity: 0,
      y: 12,
      duration: 0.4,
      ease: 'power2.out',
      delay: 0.1,
    })
    gsap.from('.results-panel', {
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
  <div class="scorecheck-query">
    <!-- CRT scanline overlay -->
    <div class="scanline-overlay" />

    <!-- Header -->
    <header class="q-header">
      <button class="back-btn" @click="goBack">
        <PhArrowLeft size="14" weight="bold" />
        BACK
      </button>
      <span class="q-title">STUDENT QUERY</span>
    </header>

    <main class="q-main">
      <!-- Search Panel -->
      <div v-if="!hasSearched || loading" class="search-panel">
        <div class="search-hero">
          <div class="hero-frame">
            <PhStudent size="32" weight="duotone" class="hero-icon" />
          </div>
          <h1 class="hero-title">QUERY RESULTS</h1>
          <p class="hero-desc">Enter student name and ID number</p>
        </div>

        <div class="search-form-card">
          <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" @submit.prevent="handleSearch">
            <div class="form-field">
              <label class="field-label">STUDENT NAME</label>
              <el-input
                v-model="formData.studentName"
                placeholder="ENTER NAME"
                size="large"
                :prefix-icon="PhStudent"
                @keydown.enter="handleSearch"
              />
            </div>
            <div class="form-field">
              <label class="field-label">ID NUMBER</label>
              <el-input
                v-model="formData.idCard"
                placeholder="ENTER ID NUMBER"
                size="large"
                :prefix-icon="PhCheckCircle"
                @keydown.enter="handleSearch"
              />
            </div>
            <button type="button" class="search-submit" :loading="loading" @click="handleSearch">
              <PhMagnifyingGlass v-if="!loading" size="16" weight="bold" />
              <span>{{ loading ? 'SEARCHING...' : 'SEARCH RESULTS' }}</span>
            </button>
          </el-form>
        </div>
      </div>

      <!-- Results Panel -->
      <div v-if="hasSearched && scores.length > 0 && !loading" class="results-panel">
        <!-- Result Banner -->
        <div class="result-banner">
          <div class="banner-line" />
          <div class="banner-content">
            <span class="banner-status">&#9679;</span>
            <span class="banner-name">{{ formData.studentName }}</span>
            <span class="banner-detail">{{ scores.length }} SUBJECTS &middot; {{ groupedScores.length }} EXAMS</span>
          </div>
          <button class="banner-action" @click="resetSearch">RESET</button>
        </div>

        <!-- Summary Stats -->
        <div class="summary-row">
          <div class="summary-item">
            <span class="summary-label">TOTAL</span>
            <span class="summary-value">{{ totalScore }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">AVERAGE</span>
            <span class="summary-value">{{ avgScore }}</span>
          </div>
        </div>

        <!-- Exam Blocks -->
        <div class="exam-blocks">
          <div
            v-for="[examName, examScores] in groupedScores"
            :key="examName"
            class="exam-block"
          >
            <div class="exam-header">
              <PhTrophy size="14" weight="duotone" class="trophy-icon" />
              <h3 class="exam-title">{{ examName }}</h3>
              <span class="exam-count">{{ examScores.length }} SUBJ</span>
            </div>
            <div class="subject-grid">
              <div
                v-for="score in examScores"
                :key="score.id"
                class="subject-row"
              >
                <span class="subj-label">{{ score.subject }}</span>
                <span class="subj-score" :style="{ color: getColor(Number(score.score)) }">
                  {{ score.score }}
                </span>
                <span class="subj-grade">{{ getGrade(Number(score.score)) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="hasSearched && scores.length === 0 && !loading" class="empty-view">
        <span class="empty-symbol">[ 404 ]</span>
        <h3>NO RECORDS FOUND</h3>
        <p>Verify name and ID number</p>
        <button class="empty-btn" @click="resetSearch">RETRY</button>
      </div>
    </main>
  </div>
</template>

<style scoped>
.scorecheck-query {
  min-height: 100dvh;
  background: var(--bg);
  position: relative;
}

/* --- Header --- */
.q-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 40px;
  background: var(--bg-deep);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 4px 10px;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.back-btn:hover {
  border-color: var(--amber);
  color: var(--amber);
}

.q-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.08em;
}

/* --- Main --- */
.q-main {
  position: relative;
  z-index: 1;
  max-width: 640px;
  margin: 0 auto;
  padding: 48px 24px 80px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* --- Search Panel --- */
.search-hero {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-frame {
  width: 56px;
  height: 56px;
  border: 1px solid var(--border);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.hero-icon {
  opacity: 0.4;
}

.hero-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.1em;
}

.hero-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.search-form-card {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 24px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.form-field:last-of-type {
  margin-bottom: 0;
}

.field-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.1em;
}

.search-submit {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 0;
  background: var(--amber);
  color: var(--text-inverse);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  box-shadow: var(--shadow-amber);
  margin-top: 8px;
}

.search-submit:hover:not(:disabled) {
  background: var(--amber-dim);
  transform: translateY(-1px);
}

.search-submit:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

/* --- Results Panel --- */
.results-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--surface);
  border: 1px solid var(--border);
}

.banner-line {
  flex: 1;
  height: 1px;
  background: var(--border);
}

.banner-status {
  color: var(--emerald);
  font-size: 10px;
  animation: breathe 3s ease-in-out infinite;
}

.banner-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.04em;
}

.banner-detail {
  font-size: 11px;
  color: var(--text-muted);
}

.banner-action {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  flex-shrink: 0;
}

.banner-action:hover {
  border-color: var(--amber);
  color: var(--amber);
}

/* Summary */
.summary-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--border);
  border: 1px solid var(--border);
}

.summary-item {
  background: var(--bg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 9px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.12em;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* Exam blocks */
.exam-blocks {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--border);
  border: 1px solid var(--border);
}

.exam-block {
  background: var(--bg);
  padding: 16px;
}

.exam-block:nth-child(even) {
  background: var(--bg-deep);
}

.exam-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}

.trophy-icon {
  color: var(--amber);
}

.exam-title {
  flex: 1;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.04em;
}

.exam-count {
  font-size: 10px;
  color: var(--text-muted);
  font-weight: 600;
}

/* Subject rows */
.subject-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.subject-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  gap: 12px;
}

.subject-row:last-child {
  border-bottom: none;
}

.subj-label {
  flex: 1;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.subj-score {
  font-size: 14px;
  font-weight: 700;
  min-width: 36px;
  text-align: right;
}

.subj-grade {
  font-size: 9px;
  font-weight: 700;
  color: var(--text-muted);
  letter-spacing: 0.06em;
  min-width: 60px;
  text-align: right;
}

/* --- Empty State --- */
.empty-view {
  padding: 60px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.empty-symbol {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.1em;
}

.empty-view h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.06em;
}

.empty-view p {
  font-size: 12px;
  color: var(--text-muted);
}

.empty-btn {
  height: 36px;
  padding: 0 20px;
  border: 1px solid var(--amber);
  border-radius: 0;
  background: transparent;
  color: var(--amber);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  margin-top: 8px;
}

.empty-btn:hover {
  background: var(--amber);
  color: var(--text-inverse);
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .q-main {
    padding: 32px 16px 60px;
  }

  .search-form-card {
    padding: 20px;
  }

  .hero-title {
    font-size: 18px;
  }

  .summary-value {
    font-size: 20px;
  }
}
</style>
