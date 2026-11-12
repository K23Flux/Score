<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { PhCheckCircle, PhUser } from '@phosphor-icons/vue'
import { studentQuery } from '@/api/score'
import type { Score } from '@/types'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const hasSearched = ref(false)
const errorMessage = ref('')
const scores = ref<Score[]>([])
const submittedName = ref('')

const formData = reactive({ studentName: '', idCard: '' })

const rules: FormRules = {
  studentName: [{ required: true, message: '请输入学生姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dX]$/, message: '身份证号应为18位，最后一位可为X', trigger: 'blur' },
  ],
}

const groupedScores = computed(() => {
  const map = new Map<string, Score[]>()
  scores.value.forEach((score) => {
    const list = map.get(score.examName) || []
    list.push(score)
    map.set(score.examName, list)
  })
  return Array.from(map.entries()).map(([examName, list]) => ({
    examName,
    scores: list,
    total: Math.round(list.reduce((sum, item) => sum + Number(item.score || 0), 0) * 100) / 100,
  }))
})

const subjectCount = computed(() => scores.value.length)

const formatScore = (value: number | string) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return Number.isInteger(number) ? String(number) : String(Math.round(number * 100) / 100)
}

const handleSearch = async () => {
  if (loading.value) return

  formData.studentName = formData.studentName.trim()
  formData.idCard = formData.idCard.trim().toUpperCase()

  formRef.value?.clearValidate()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  errorMessage.value = ''
  hasSearched.value = false
  try {
    const res = await studentQuery({
      studentName: formData.studentName,
      idCard: formData.idCard,
    })
    scores.value = res.data?.score || []
    submittedName.value = formData.studentName
    hasSearched.value = true
  } catch (error) {
    scores.value = []
    const message = error instanceof Error ? error.message : ''
    // 后端用业务错误表示“姓名或身份证号不匹配”，应展示为空结果而不是系统故障
    if (message.includes('姓名或身份证号错误')) {
      submittedName.value = formData.studentName
      hasSearched.value = true
    } else {
      errorMessage.value = '成绩查询失败，请稍后重试。'
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  hasSearched.value = false
  errorMessage.value = ''
  scores.value = []
  submittedName.value = ''
  formData.studentName = ''
  formData.idCard = ''
}

const goToLogin = () => router.push('/login')

onMounted(() => {
  document.title = '学生成绩查询 · 成绩查询平台'
})
</script>

<template>
  <div class="query-page">
    <div class="query-wrap">
      <div class="query-card">
        <div class="brand-line">
          <strong>成绩查询平台</strong>
          <span>Score</span>
        </div>

        <h1>学生成绩查询</h1>
        <p class="page-desc">请输入姓名和身份证号查询成绩</p>

        <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" @submit.prevent="handleSearch">
          <el-form-item label="姓名" prop="studentName">
            <el-input
              v-model="formData.studentName"
              :prefix-icon="PhUser"
              autocomplete="name"
              placeholder="请输入姓名"
              @keydown.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="身份证号" prop="idCard">
            <el-input
              v-model="formData.idCard"
              :prefix-icon="PhCheckCircle"
              inputmode="numeric"
              maxlength="18"
              placeholder="请输入18位身份证号"
              @keydown.enter="handleSearch"
            />
          </el-form-item>

          <el-button class="query-submit" type="primary" :loading="loading" @click="handleSearch">
            查询成绩
          </el-button>
        </el-form>

        <div class="query-foot">
          <button class="link-btn" type="button" @click="goToLogin">教师登录</button>
        </div>
      </div>

      <div v-if="loading" class="panel state-panel">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="errorMessage" class="panel state-panel">
        <h2>成绩查询失败</h2>
        <p>{{ errorMessage }}</p>
        <el-button @click="handleSearch">重试</el-button>
      </div>

      <div v-else-if="hasSearched && scores.length === 0" class="panel state-panel">
        <h2>未查询到成绩</h2>
        <p>请核对姓名和身份证号后重试。</p>
        <el-button @click="resetSearch">重新查询</el-button>
      </div>

      <div v-else-if="hasSearched" class="panel result-panel">
        <div class="result-head">
          <div>
            <h2>{{ submittedName }}</h2>
            <p class="result-meta">{{ groupedScores.length }} 场考试，{{ subjectCount }} 条科目成绩</p>
          </div>
          <el-button @click="resetSearch">重新查询</el-button>
        </div>

        <div v-for="exam in groupedScores" :key="exam.examName" class="exam-block">
          <div class="exam-top">
            <h3>{{ exam.examName }}</h3>
            <div class="exam-total">本场总分 <strong>{{ formatScore(exam.total) }}</strong></div>
          </div>
          <div class="subject-list">
            <div v-for="(item, index) in exam.scores" :key="item.id || index" class="subject-item">
              <span>{{ item.subject }}</span>
              <b>{{ formatScore(item.score) }}</b>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.query-page {
  min-height: 100vh;
  padding: 40px 24px;
  background: var(--color-bg);
}

.query-wrap {
  width: min(720px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.query-card {
  width: min(440px, 100%);
  padding: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
}

.brand-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 28px;
}

.brand-line strong {
  font-size: 16px;
  font-weight: 600;
}

.brand-line span {
  font-size: 12px;
  color: var(--color-text-muted);
}

h1 {
  margin: 0 0 6px;
  font-size: 26px;
  line-height: 1.3;
  font-weight: 600;
  color: var(--color-text);
}

.page-desc {
  margin: 0 0 24px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.query-submit {
  width: 100%;
  height: 40px;
  margin-top: 4px;
}

.query-foot {
  margin-top: 20px;
  text-align: center;
}

.link-btn {
  border: 0;
  background: none;
  padding: 0;
  color: var(--color-primary);
  font-size: 13px;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.link-btn:hover {
  color: var(--color-primary-hover);
}

.panel {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-surface);
}

.state-panel {
  padding: 40px 24px;
  text-align: center;
}

.state-panel h2 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}

.state-panel p {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.result-panel {
  padding: 20px;
}

.result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.result-head h2 {
  margin: 0 0 2px;
  font-size: 18px;
  font-weight: 600;
}

.result-meta {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.exam-block {
  padding: 18px 0;
  border-bottom: 1px solid var(--color-border);
}

.exam-block:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.exam-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.exam-top h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.exam-total {
  font-size: 13px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.exam-total strong {
  margin-left: 4px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
}

.subject-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 0 20px;
}

.subject-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f3f7;
  font-size: 14px;
}

.subject-item span {
  color: var(--color-text-secondary);
}

.subject-item b {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 767px) {
  .query-page {
    padding: 24px 16px;
  }

  .query-card {
    padding: 24px;
  }

  h1 {
    font-size: 23px;
  }

  .result-panel {
    padding: 16px;
  }

  .result-head {
    flex-wrap: wrap;
  }
}
</style>
