<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { PhFileCsv, PhUploadSimple } from '@phosphor-icons/vue'
import { importScore, importStudent } from '@/api/import'
import { useAuthStore } from '@/stores/auth'

type ImportTab = 'student' | 'score'

const authStore = useAuthStore()

const activeTab = ref<ImportTab>('student')
const studentFormRef = ref<FormInstance>()
const scoreFormRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const fileError = ref('')
const importing = ref(false)
const dragActive = ref(false)
const resultMessage = ref('')
const resultType = ref<'success' | 'warning' | 'error' | ''>('')

const studentForm = reactive({ className: authStore.className || '' })
const scoreForm = reactive({ examName: '', className: authStore.className || '' })

const studentRules: FormRules = {
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
}

const scoreRules: FormRules = {
  examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
}

const activeHint = computed(() => activeTab.value === 'student'
  ? '学生表表头：姓名、身份证；班级以上方填写为准。'
  : '成绩表第一列：姓名，其余列为科目。请先导入学生，再导入成绩。')

const fileSizeText = computed(() => {
  if (!selectedFile.value) return ''
  const bytes = selectedFile.value.size
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
})

const switchTab = (tab: ImportTab) => {
  if (importing.value || activeTab.value === tab) return
  activeTab.value = tab
  selectedFile.value = null
  fileError.value = ''
  resultMessage.value = ''
  resultType.value = ''
}

const validateFile = (file: File) => {
  const name = file.name.toLowerCase()
  if (!/\.(xlsx|xls)$/.test(name)) return '仅支持 .xlsx 或 .xls 文件'
  if (file.size === 0) return '文件为空，请重新选择'
  if (file.size > 10 * 1024 * 1024) return '文件大小不能超过 10MB'
  return ''
}

const acceptFile = (file: File) => {
  const error = validateFile(file)
  if (error) {
    selectedFile.value = null
    fileError.value = error
    return
  }
  selectedFile.value = file
  fileError.value = ''
  resultMessage.value = ''
  resultType.value = ''
}

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (files.length > 1) {
    selectedFile.value = null
    fileError.value = '一次只能导入一个文件'
  } else if (files[0]) {
    acceptFile(files[0])
  }
  target.value = ''
}

const handleDrop = (event: DragEvent) => {
  dragActive.value = false
  if (importing.value) return
  const files = Array.from(event.dataTransfer?.files || [])
  if (files.length > 1) {
    selectedFile.value = null
    fileError.value = '一次只能导入一个文件'
    return
  }
  if (files[0]) acceptFile(files[0])
}

const triggerFilePicker = () => {
  if (importing.value) return
  fileInput.value?.click()
}

const removeFile = () => {
  if (importing.value) return
  selectedFile.value = null
  fileError.value = ''
  resultMessage.value = ''
  resultType.value = ''
}

const handleImport = async () => {
  if (importing.value) return

  const formRef = activeTab.value === 'student' ? studentFormRef.value : scoreFormRef.value
  const form = activeTab.value === 'student' ? studentForm : scoreForm

  form.className = form.className.trim()
  if (activeTab.value === 'score') scoreForm.examName = scoreForm.examName.trim()

  formRef?.clearValidate()
  const valid = await formRef?.validate().catch(() => false)
  if (!valid) return

  if (!selectedFile.value) {
    fileError.value = '请选择 Excel 文件'
    return
  }

  const error = validateFile(selectedFile.value)
  if (error) {
    fileError.value = error
    return
  }

  importing.value = true
  resultMessage.value = ''
  resultType.value = ''

  try {
    const file = selectedFile.value
    const res = activeTab.value === 'student'
      ? await importStudent(file, studentForm.className)
      : await importScore(file, scoreForm.examName, scoreForm.className)

    const message = typeof res.data === 'string' && res.data
      ? res.data
      : (activeTab.value === 'student' ? '学生信息导入成功' : '成绩导入成功')

    resultMessage.value = message
    resultType.value = /(^|\D)0\s*条/.test(message) ? 'warning' : 'success'
    selectedFile.value = null
    if (resultType.value === 'warning') ElMessage.warning('导入完成，请检查结果')
    else ElMessage.success('导入完成')
  } catch {
    resultMessage.value = '导入失败，请检查文件内容后重试。'
    resultType.value = 'error'
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  document.title = '数据导入 · 成绩查询平台'
})
</script>

<template>
  <section class="import-page">
    <header class="page-head">
      <div>
        <h1>数据导入</h1>
        <p>先导入学生信息，再导入考试成绩</p>
      </div>
    </header>

    <div class="panel">
      <div class="tabs" role="tablist" aria-label="导入类型">
        <button
          class="tab"
          type="button"
          role="tab"
          :aria-selected="activeTab === 'student'"
          :disabled="importing"
          @click="switchTab('student')"
        >
          学生信息
        </button>
        <button
          class="tab"
          type="button"
          role="tab"
          :aria-selected="activeTab === 'score'"
          :disabled="importing"
          @click="switchTab('score')"
        >
          考试成绩
        </button>
      </div>

      <div class="form-panel">
        <el-form
          v-show="activeTab === 'student'"
          ref="studentFormRef"
          :model="studentForm"
          :rules="studentRules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="班级" prop="className">
            <el-input
              v-model="studentForm.className"
              :disabled="importing"
              placeholder="如：高一(2)班"
            />
          </el-form-item>
        </el-form>

        <el-form
          v-show="activeTab === 'score'"
          ref="scoreFormRef"
          :model="scoreForm"
          :rules="scoreRules"
          label-position="top"
          @submit.prevent
        >
          <div class="form-grid">
            <el-form-item label="考试名称" prop="examName">
              <el-input
                v-model="scoreForm.examName"
                :disabled="importing"
                placeholder="如：期中考试"
              />
            </el-form-item>
            <el-form-item label="班级" prop="className">
              <el-input
                v-model="scoreForm.className"
                :disabled="importing"
                placeholder="如：高一(2)班"
              />
            </el-form-item>
          </div>
        </el-form>

        <div
          class="dropzone"
          :class="{ 'is-drag': dragActive, 'is-disabled': importing }"
          role="button"
          tabindex="0"
          aria-label="选择或拖入 Excel 文件"
          @click="triggerFilePicker"
          @keydown.enter.prevent="triggerFilePicker"
          @keydown.space.prevent="triggerFilePicker"
          @dragenter.prevent="dragActive = !importing"
          @dragover.prevent="dragActive = !importing"
          @dragleave.prevent="dragActive = false"
          @drop.prevent="handleDrop"
        >
          <PhUploadSimple size="26" weight="duotone" class="drop-icon" />
          <p><strong>选择文件</strong> 或拖到这里</p>
          <p class="hint">支持 .xlsx / .xls，单个文件不超过 10MB</p>
        </div>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="handleFileChange" />

        <div v-if="selectedFile" class="file-row">
          <PhFileCsv size="18" weight="duotone" class="file-icon" />
          <span class="file-name" :title="selectedFile.name">{{ selectedFile.name }}</span>
          <span class="file-size">{{ fileSizeText }}</span>
          <button class="file-remove" type="button" :disabled="importing" @click="removeFile">移除</button>
        </div>

        <p v-if="fileError" class="file-error">{{ fileError }}</p>

        <div class="form-actions">
          <el-button type="primary" :loading="importing" :disabled="importing" @click="handleImport">
            导入
          </el-button>
          <span class="form-note">导入过程中请勿重复提交</span>
        </div>

        <div v-if="resultMessage" class="result-box" :class="resultType">
          {{ resultMessage }}
        </div>

        <p class="form-hint">{{ activeHint }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.import-page {
  max-width: 1400px;
}

.page-head {
  margin-bottom: 20px;
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
  overflow: hidden;
}

.tabs {
  display: flex;
  gap: 4px;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
}

.tab {
  border: 0;
  border-bottom: 2px solid transparent;
  background: none;
  padding: 12px 12px 10px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.tab:hover:not(:disabled) {
  color: var(--color-text);
}

.tab[aria-selected="true"] {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

.tab:disabled {
  cursor: not-allowed;
  opacity: .6;
}

.form-panel {
  padding: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.dropzone {
  margin-top: 4px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-panel);
  background: #f8fafc;
  transition: border-color .15s ease, background-color .15s ease;
}

.dropzone:hover,
.dropzone.is-drag {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.dropzone.is-disabled {
  cursor: not-allowed;
  opacity: .6;
}

.drop-icon {
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.dropzone p {
  margin: 0;
}

.dropzone strong {
  color: var(--color-primary);
  font-weight: 600;
}

.dropzone .hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
}

.file-icon {
  flex-shrink: 0;
  color: var(--color-text-secondary);
}

.file-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.file-remove {
  border: 0;
  background: none;
  padding: 2px 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
  border-radius: 4px;
}

.file-remove:hover:not(:disabled) {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.file-error {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--color-danger);
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.form-note {
  font-size: 12px;
  color: var(--color-text-muted);
}

.form-hint {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}

.result-box {
  margin-top: 16px;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: #f8fafc;
  font-size: 13px;
}

.result-box.success {
  border-color: #b7e1c8;
  background: #f3fbf6;
  color: var(--color-success);
}

.result-box.warning {
  border-color: #f0d9a8;
  background: #fffaf0;
  color: var(--color-warning);
}

.result-box.error {
  border-color: #f3c1bd;
  background: #fff6f5;
  color: var(--color-danger);
}

@media (max-width: 767px) {
  .page-head h1 {
    font-size: 23px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-panel {
    padding: 16px;
  }
}
</style>
