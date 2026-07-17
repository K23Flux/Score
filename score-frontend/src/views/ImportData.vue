<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { importStudent, importScore } from '@/api/import'
import { ElMessage } from 'element-plus'
import { PhUploadSimple, PhStudent, PhExam, PhFileCsv } from '@phosphor-icons/vue'
import { useAuthStore } from '@/stores/auth'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const authStore = useAuthStore()
const activeTab = ref('student')
const studentLoading = ref(false)
const scoreLoading = ref(false)
const studentForm = reactive({ className: authStore.className || '' })
const scoreForm = reactive({ examName: '', className: authStore.className || '' })
const studentFileList = ref<any[]>([])
const scoreFileList = ref<any[]>([])
const studentDrag = ref(false)
const scoreDrag = ref(false)

const handleStudentDrop = (e: DragEvent) => {
  studentDrag.value = false
  const f = e.dataTransfer?.files[0]
  if (f) studentFileList.value = [{ raw: f, name: f.name }]
}

const handleScoreDrop = (e: DragEvent) => {
  scoreDrag.value = false
  const f = e.dataTransfer?.files[0]
  if (f) scoreFileList.value = [{ raw: f, name: f.name }]
}

const handleStudentImport = async () => {
  if (!studentForm.className) { ElMessage.warning('INPUT CLASS NAME'); return }
  if (studentFileList.value.length === 0) { ElMessage.warning('NO FILE SELECTED'); return }
  studentLoading.value = true
  try {
    await importStudent(studentFileList.value[0].raw, studentForm.className)
    ElMessage.success('STUDENT DATA IMPORTED')
    studentFileList.value = []
    studentForm.className = ''
  } catch {}
  finally { studentLoading.value = false }
}

const handleScoreImport = async () => {
  if (!scoreForm.examName) { ElMessage.warning('INPUT EXAM NAME'); return }
  if (!scoreForm.className) { ElMessage.warning('INPUT CLASS NAME'); return }
  if (scoreFileList.value.length === 0) { ElMessage.warning('NO FILE SELECTED'); return }
  scoreLoading.value = true
  try {
    await importScore(scoreFileList.value[0].raw, scoreForm.examName, scoreForm.className)
    ElMessage.success('SCORE DATA IMPORTED')
    scoreFileList.value = []
    scoreForm.examName = ''
    scoreForm.className = ''
  } catch {}
  finally { scoreLoading.value = false }
}

const beforeUpload = (file: any) => {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isExcel) { ElMessage.error('EXCEL FILES ONLY (.xlsx/.xls)') }
  if (!isLt10M) { ElMessage.error('FILE SIZE MUST BE < 10MB') }
  return isExcel && isLt10M
}

let ctx: { revert: () => void } | null = null

onMounted(() => {
  document.title = 'ScoreCheck — Import'

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReduced) return

  ctx = gsap.context(() => {
    gsap.from('.tab-bar', {
      opacity: 0,
      y: 10,
      duration: 0.3,
      ease: 'power2.out',
      delay: 0.1,
    })
    gsap.from('.import-flow', {
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
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <span class="header-bracket">[</span>
        <h1 class="header-title">DATA IMPORT</h1>
        <span class="header-bracket">]</span>
      </div>
      <div class="header-meta">
        <span class="meta-dot" />
        <span class="meta-text">BATCH UPLOAD SYSTEM</span>
      </div>
    </div>

    <!-- Tab Bar -->
    <div class="tab-bar">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'student' }"
        @click="activeTab = 'student'"
      >
        <PhStudent size="14" weight="regular" />
        <span>STUDENT DATA</span>
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'score' }"
        @click="activeTab = 'score'"
      >
        <PhExam size="14" weight="regular" />
        <span>SCORE DATA</span>
      </button>
    </div>

    <!-- Student Import Flow -->
    <div v-show="activeTab === 'student'" class="import-flow animate-fade-in">
      <!-- Step 1: Class Info -->
      <div class="step-card">
        <div class="step-marker">01</div>
        <div class="step-body">
          <h3 class="step-title">CLASS INFORMATION</h3>
          <p class="step-desc">Specify class for data grouping</p>
          <el-input
            v-model="studentForm.className"
            placeholder="e.g. 2026 Grade Class 1"
            size="large"
          />
        </div>
      </div>

      <!-- Step 2: File Drop Zone -->
      <div class="step-card">
        <div class="step-marker">02</div>
        <div class="step-body">
          <h3 class="step-title">SELECT FILE</h3>
          <p class="step-desc">Upload Excel file with student records</p>
          <div
            class="drop-zone"
            :class="{ 'drop-active': studentDrag }"
            @dragover.prevent="studentDrag = true"
            @dragleave.prevent="studentDrag = false"
            @drop.prevent="handleStudentDrop"
          >
            <PhFileCsv size="28" weight="duotone" class="drop-icon" />
            <p class="drop-text">DRAG FILE HERE OR</p>
            <el-upload
              v-model:file-list="studentFileList"
              class="upload-btn"
              :auto-upload="false"
              accept=".xlsx,.xls"
              :before-upload="beforeUpload"
              :limit="1"
            >
              <el-button size="default" type="primary" class="amber-outline">SELECT FILE</el-button>
            </el-upload>
            <p v-if="studentFileList.length > 0" class="file-name">
              <span class="file-prefix">&gt;</span> {{ studentFileList[0].name }}
            </p>
            <p class="drop-hint">.xlsx / .xls &middot; MAX 10MB</p>
          </div>
        </div>
      </div>

      <!-- Step 3: Submit -->
      <button
        class="submit-btn"
        :loading="studentLoading"
        @click="handleStudentImport"
      >
        <PhUploadSimple v-if="!studentLoading" size="16" weight="bold" />
        <span>{{ studentLoading ? 'PROCESSING...' : 'IMPORT STUDENT DATA' }}</span>
      </button>
    </div>

    <!-- Score Import Flow -->
    <div v-show="activeTab === 'score'" class="import-flow animate-fade-in">
      <!-- Step 1: Exam Info -->
      <div class="step-card">
        <div class="step-marker">01</div>
        <div class="step-body">
          <h3 class="step-title">EXAM INFORMATION</h3>
          <p class="step-desc">Specify exam and class details</p>
          <el-form :model="scoreForm" label-position="top" class="step-form">
            <el-form-item label="EXAM NAME">
              <el-input v-model="scoreForm.examName" placeholder="e.g. 2026 Semester Midterm" size="large" />
            </el-form-item>
            <el-form-item label="CLASS NAME">
              <el-input v-model="scoreForm.className" placeholder="e.g. 2026 Grade Class 1" size="large" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- Step 2: File Drop Zone -->
      <div class="step-card">
        <div class="step-marker">02</div>
        <div class="step-body">
          <h3 class="step-title">SELECT FILE</h3>
          <p class="step-desc">Upload Excel file with score records</p>
          <div
            class="drop-zone"
            :class="{ 'drop-active': scoreDrag }"
            @dragover.prevent="scoreDrag = true"
            @dragleave.prevent="scoreDrag = false"
            @drop.prevent="handleScoreDrop"
          >
            <PhFileCsv size="28" weight="duotone" class="drop-icon" />
            <p class="drop-text">DRAG FILE HERE OR</p>
            <el-upload
              v-model:file-list="scoreFileList"
              class="upload-btn"
              :auto-upload="false"
              accept=".xlsx,.xls"
              :before-upload="beforeUpload"
              :limit="1"
            >
              <el-button size="default" type="primary" class="amber-outline">SELECT FILE</el-button>
            </el-upload>
            <p v-if="scoreFileList.length > 0" class="file-name">
              <span class="file-prefix">&gt;</span> {{ scoreFileList[0].name }}
            </p>
            <p class="drop-hint">.xlsx / .xls &middot; MAX 10MB</p>
          </div>
        </div>
      </div>

      <!-- Step 3: Submit -->
      <button
        class="submit-btn"
        :loading="scoreLoading"
        @click="handleScoreImport"
      >
        <PhUploadSimple v-if="!scoreLoading" size="16" weight="bold" />
        <span>{{ scoreLoading ? 'PROCESSING...' : 'IMPORT SCORE DATA' }}</span>
      </button>
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

/* --- Tab Bar --- */
.tab-bar {
  display: flex;
  gap: 2px;
  background: var(--border);
  padding: 2px;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.tab-btn:hover {
  color: var(--text-secondary);
  background: var(--surface);
}

.tab-btn.active {
  color: var(--amber);
  background: var(--bg);
  font-weight: 600;
}

/* --- Import Flow --- */
.import-flow {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 560px;
  margin: 0 auto;
  width: 100%;
}

.step-card {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 20px;
  display: flex;
  gap: 16px;
}

.step-marker {
  font-size: 12px;
  font-weight: 700;
  color: var(--amber);
  letter-spacing: 0.05em;
  flex-shrink: 0;
  padding-top: 2px;
}

.step-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.05em;
}

.step-desc {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.03em;
}

.step-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-form :deep(.el-form-item__label) {
  font-size: 10px !important;
  font-weight: 600 !important;
  letter-spacing: 0.08em !important;
  color: var(--text-muted) !important;
}

.step-form :deep(.el-input__wrapper) {
  background: var(--bg-deep);
  border-radius: 0;
}

/* --- Drop Zone --- */
.drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px;
  border: 1px dashed var(--border-strong);
  background: var(--bg-deep);
  transition: all var(--fast) var(--ease-out);
}

.drop-active {
  border-color: var(--amber);
  background: var(--amber-bg);
}

.drop-icon {
  color: var(--amber);
}

.drop-text {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  letter-spacing: 0.06em;
}

.file-name {
  font-size: 12px;
  color: var(--amber);
  font-weight: 600;
  background: var(--amber-bg);
  padding: 4px 12px;
}

.file-prefix {
  margin-right: 4px;
  opacity: 0.6;
}

.drop-hint {
  font-size: 10px;
  color: var(--text-muted);
  letter-spacing: 0.05em;
}

/* --- Submit Button --- */
.submit-btn {
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
  gap: 10px;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  box-shadow: var(--shadow-amber);
  max-width: 560px;
  margin: 0 auto;
}

.submit-btn:hover:not(:disabled) {
  background: var(--amber-dim);
  box-shadow: 0 6px 24px rgba(240, 165, 0, 0.3);
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .step-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .step-form {
    width: 100%;
  }
}
</style>
