<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { PhFileCsv, PhUploadSimple } from '@phosphor-icons/vue'
import {
  commitScoreImport,
  commitStudentImport,
  previewScoreImport,
  previewStudentImport,
} from '@/api/import'
import { useAuthStore } from '@/stores/auth'
import type { ImportMapping, ImportPreview } from '@/types'

type ImportTab = 'student' | 'score'

const authStore = useAuthStore()

const activeTab = ref<ImportTab>('student')
const studentFormRef = ref<FormInstance>()
const scoreFormRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const fileError = ref('')
const previewing = ref(false)
const importing = ref(false)
const dragActive = ref(false)
const preview = ref<ImportPreview | null>(null)
const resultMessage = ref('')
const resultType = ref<'success' | 'warning' | 'error' | ''>('')

const mapping = reactive<ImportMapping>({
  nameColumn: null,
  idColumn: null,
  subjectColumns: [],
  ignoredColumns: [],
})

let previewSeq = 0
let previewTimer: ReturnType<typeof setTimeout> | null = null

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
  ? '表头会自动识别“姓名”“身份证”等常见写法；解析后可手动调整列映射。班级以上方填写为准。'
  : '表头会自动识别姓名和科目列；解析后可手动调整。缺考请留空，不要填“缺考”。')

const mappingSourceText = computed(() => {
  if (!preview.value) return ''
  if (preview.value.mappingSource === 'ai') return 'AI 建议的列映射，请确认'
  if (preview.value.mappingSource === 'manual') return '手动调整的列映射'
  return '按表头规则识别的列映射'
})

const subjectOptions = computed(() => {
  if (!preview.value) return []
  return preview.value.columns.filter(
    (column) => column.index !== mapping.nameColumn && column.index !== mapping.idColumn,
  )
})

const canCommit = computed(() => {
  if (!preview.value || preview.value.headerErrors.length > 0) return false
  if (mapping.nameColumn === null || preview.value.validRows === 0) return false
  if (activeTab.value === 'student') return mapping.idColumn !== null
  return mapping.subjectColumns.length > 0
})

const visibleErrors = computed(() => preview.value?.errors.slice(0, 20) || [])

const activeClassName = () => (activeTab.value === 'student' ? studentForm.className : scoreForm.className)

const validateFile = (file: File) => {
  const name = file.name.toLowerCase()
  if (!/\.(xlsx|xls)$/.test(name)) return '仅支持 .xlsx 或 .xls 文件'
  if (file.size === 0) return '文件为空，请重新选择'
  if (file.size > 10 * 1024 * 1024) return '文件大小不能超过 10MB'
  return ''
}

const resetPreview = () => {
  preview.value = null
  selectedFile.value = null
  fileError.value = ''
  mapping.nameColumn = null
  mapping.idColumn = null
  mapping.subjectColumns = []
  mapping.ignoredColumns = []
}

const resetAll = () => {
  resetPreview()
  resultMessage.value = ''
  resultType.value = ''
}

const switchTab = (tab: ImportTab) => {
  if (importing.value || previewing.value || activeTab.value === tab) return
  activeTab.value = tab
  resetAll()
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
  preview.value = null
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
  if (importing.value || previewing.value) return
  const files = Array.from(event.dataTransfer?.files || [])
  if (files.length > 1) {
    selectedFile.value = null
    fileError.value = '一次只能导入一个文件'
    return
  }
  if (files[0]) acceptFile(files[0])
}

const triggerFilePicker = () => {
  if (importing.value || previewing.value) return
  fileInput.value?.click()
}

const removeFile = () => {
  if (importing.value || previewing.value) return
  resetPreview()
  resultMessage.value = ''
  resultType.value = ''
}

const validateActiveForm = async () => {
  const form = activeTab.value === 'student' ? studentForm : scoreForm
  form.className = form.className.trim()
  if (activeTab.value === 'score') scoreForm.examName = scoreForm.examName.trim()
  const formRef = activeTab.value === 'student' ? studentFormRef.value : scoreFormRef.value
  formRef?.clearValidate()
  return (await formRef?.validate().catch(() => false)) === true
}

const mappingPayload = (): ImportMapping => ({
  nameColumn: mapping.nameColumn,
  idColumn: mapping.idColumn === mapping.nameColumn ? null : mapping.idColumn,
  subjectColumns: mapping.subjectColumns.filter(
    (index) => index !== mapping.nameColumn && index !== mapping.idColumn,
  ),
  ignoredColumns: [],
})

const fetchPreview = async (withMapping: boolean) => {
  const file = selectedFile.value
  if (!file) return
  const seq = ++previewSeq
  previewing.value = true
  fileError.value = ''
  try {
    const className = activeClassName()
    const res = activeTab.value === 'student'
      ? await previewStudentImport(file, className, withMapping ? mappingPayload() : undefined)
      : await previewScoreImport(file, scoreForm.examName, className, withMapping ? mappingPayload() : undefined)
    if (seq !== previewSeq) return
    preview.value = res.data
    Object.assign(mapping, res.data.mapping)
    resultMessage.value = ''
    resultType.value = ''
  } catch (error) {
    if (seq !== previewSeq) return
    preview.value = null
    fileError.value = error instanceof Error ? error.message : '解析失败，请检查文件'
  } finally {
    if (seq === previewSeq) previewing.value = false
  }
}

const parsePreview = async () => {
  if (previewing.value || importing.value) return
  if (!selectedFile.value) {
    fileError.value = '请选择 Excel 文件'
    return
  }
  if (!(await validateActiveForm())) return
  await fetchPreview(false)
}

const refreshPreview = () => {
  if (!preview.value || previewing.value || importing.value) return
  if (previewTimer) clearTimeout(previewTimer)
  previewTimer = setTimeout(() => {
    void fetchPreview(true)
  }, 300)
}

const confirmImport = async () => {
  if (importing.value || previewing.value || !preview.value || !selectedFile.value) return
  if (!canCommit.value) return

  const invalid = preview.value.invalidRows
  let ignoreErrors = false
  if (invalid > 0) {
    try {
      await ElMessageBox.confirm(
        `有 ${invalid} 行存在问题，将只导入有效行。是否继续？`,
        '存在问题的数据',
        {
          confirmButtonText: '只导入有效行',
          cancelButtonText: '返回检查',
          type: 'warning',
        },
      )
      ignoreErrors = true
    } catch {
      return
    }
  }

  importing.value = true
  try {
    const file = selectedFile.value
    const className = activeClassName()
    const res = activeTab.value === 'student'
      ? await commitStudentImport(file, className, mappingPayload(), ignoreErrors)
      : await commitScoreImport(file, scoreForm.examName, className, mappingPayload(), ignoreErrors)
    const message = res.data?.message || '导入完成'
    resetPreview()
    resultMessage.value = message
    resultType.value = 'success'
    ElMessage.success('导入完成')
  } catch (error) {
    resultMessage.value = error instanceof Error ? error.message : '导入失败，请检查文件后重试。'
    resultType.value = 'error'
  } finally {
    importing.value = false
  }
}

const formatSubjects = (subjects: Record<string, string>) => {
  const entries = Object.entries(subjects || {})
  return entries.length ? entries.map(([subject, value]) => `${subject} ${value}`).join('；') : '—'
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
          :disabled="importing || previewing"
          @click="switchTab('student')"
        >
          学生信息
        </button>
        <button
          class="tab"
          type="button"
          role="tab"
          :aria-selected="activeTab === 'score'"
          :disabled="importing || previewing"
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
              :disabled="importing || previewing || !!preview"
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
                :disabled="importing || previewing || !!preview"
                placeholder="如：期中考试"
              />
            </el-form-item>
            <el-form-item label="班级" prop="className">
              <el-input
                v-model="scoreForm.className"
                :disabled="importing || previewing || !!preview"
                placeholder="如：高一(2)班"
              />
            </el-form-item>
          </div>
        </el-form>

        <template v-if="!preview">
          <div
            class="dropzone"
            :class="{ 'is-drag': dragActive, 'is-disabled': importing || previewing }"
            role="button"
            tabindex="0"
            aria-label="选择或拖入 Excel 文件"
            @click="triggerFilePicker"
            @keydown.enter.prevent="triggerFilePicker"
            @keydown.space.prevent="triggerFilePicker"
            @dragenter.prevent="dragActive = !importing && !previewing"
            @dragover.prevent="dragActive = !importing && !previewing"
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
            <span class="file-size">{{ (selectedFile.size / 1024).toFixed(1) }} KB</span>
            <button class="file-remove" type="button" :disabled="previewing" @click="removeFile">移除</button>
          </div>

          <p v-if="fileError" class="file-error">{{ fileError }}</p>

          <div class="form-actions">
            <el-button
              type="primary"
              :loading="previewing"
              :disabled="previewing || !selectedFile"
              @click="parsePreview"
            >
              解析预览
            </el-button>
            <span class="form-note">解析后会显示列映射和逐行校验结果</span>
          </div>

          <p class="form-hint">{{ activeHint }}</p>
          <p class="form-hint">AI 表头识别由服务器配置，未启用时使用规则识别。</p>
        </template>

        <template v-else>
          <div class="preview-head">
            <div>
              <h2>确认列映射</h2>
              <p>
                {{ mappingSourceText }}；共 {{ preview.totalRows }} 行，
                有效 {{ preview.validRows }}，问题 {{ preview.invalidRows }}
              </p>
              <p class="ai-state">
                {{ preview.aiEnabled ? 'AI 表头识别已启用' : 'AI 表头识别未启用（由服务器配置）' }}
              </p>
            </div>
            <el-button link :disabled="importing" @click="removeFile">重新选择文件</el-button>
          </div>

          <div class="mapping-grid">
            <el-form-item label="姓名列">
              <el-select
                v-model="mapping.nameColumn"
                :disabled="previewing || importing"
                placeholder="请选择姓名列"
                @change="refreshPreview"
              >
                <el-option
                  v-for="column in preview.columns"
                  :key="column.index"
                  :label="column.header || `第 ${column.index + 1} 列`"
                  :value="column.index"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="activeTab === 'student'" label="身份证列">
              <el-select
                v-model="mapping.idColumn"
                :disabled="previewing || importing"
                placeholder="请选择身份证列"
                @change="refreshPreview"
              >
                <el-option
                  v-for="column in preview.columns"
                  :key="column.index"
                  :label="column.header || `第 ${column.index + 1} 列`"
                  :value="column.index"
                  :disabled="column.index === mapping.nameColumn"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-else label="科目列">
              <el-select
                v-model="mapping.subjectColumns"
                multiple
                collapse-tags
                collapse-tags-tooltip
                :disabled="previewing || importing"
                placeholder="请选择科目列"
                @change="refreshPreview"
              >
                <el-option
                  v-for="column in subjectOptions"
                  :key="column.index"
                  :label="column.header || `第 ${column.index + 1} 列`"
                  :value="column.index"
                />
              </el-select>
            </el-form-item>
          </div>

          <div v-if="preview.headerErrors.length" class="error-box">
            <p v-for="error in preview.headerErrors" :key="error">{{ error }}</p>
          </div>

          <el-table
            v-loading="previewing"
            :data="preview.rows"
            empty-text="没有可预览的数据"
            max-height="360"
            style="width: 100%"
          >
            <el-table-column prop="rowNumber" label="行" width="64" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column prop="idCard" label="身份证" width="180" />
            <el-table-column v-if="activeTab === 'student'" prop="className" label="班级" />
            <el-table-column v-else label="科目成绩" min-width="240">
              <template #default="{ row }">{{ formatSubjects(row.subjects) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="240">
              <template #default="{ row }">
                <span :class="row.valid ? 'status-ok' : 'status-bad'">
                  {{ row.valid ? '可导入' : row.message }}
                </span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="visibleErrors.length" class="error-list">
            <p>问题明细：</p>
            <ul>
              <li v-for="error in visibleErrors" :key="`${error.rowNumber}-${error.message}`">
                第 {{ error.rowNumber }} 行：{{ error.message }}
              </li>
            </ul>
            <p v-if="preview.errors.length > visibleErrors.length" class="more-errors">
              仅显示前 {{ visibleErrors.length }} 条，共 {{ preview.errors.length }} 条。
            </p>
          </div>

          <p v-if="fileError" class="file-error">{{ fileError }}</p>

          <div class="form-actions">
            <el-button
              type="primary"
              :loading="importing"
              :disabled="importing || previewing || !canCommit"
              @click="confirmImport"
            >
              确认导入
            </el-button>
            <el-button :disabled="importing || previewing" @click="removeFile">返回重选</el-button>
            <span class="form-note">确认前请核对姓名、身份证和科目列</span>
          </div>
          <p v-if="!canCommit" class="form-hint">请先补全列映射并确保至少有一行有效数据。</p>
        </template>

        <div v-if="resultMessage" class="result-box" :class="resultType">
          {{ resultMessage }}
        </div>
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

.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-head h2 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.preview-head p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.preview-head .ai-state {
  margin-top: 4px;
  font-size: 12px;
}

.mapping-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.mapping-grid :deep(.el-select) {
  width: 100%;
}

.error-box {
  margin: 4px 0 12px;
  padding: 10px 12px;
  border: 1px solid #f3c1bd;
  border-radius: var(--radius-control);
  background: #fff6f5;
  color: var(--color-danger);
  font-size: 13px;
}

.error-box p {
  margin: 0;
}

.error-list {
  margin-top: 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.error-list p {
  margin: 0;
}

.error-list ul {
  margin: 6px 0 0;
  padding-left: 18px;
  max-height: 160px;
  overflow: auto;
}

.error-list li {
  margin: 2px 0;
}

.more-errors {
  margin-top: 6px !important;
  font-size: 12px;
  color: var(--color-text-muted);
}

.status-ok {
  color: var(--color-success);
}

.status-bad {
  color: var(--color-danger);
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

  .form-grid,
  .mapping-grid {
    grid-template-columns: 1fr;
  }

  .form-panel {
    padding: 16px;
  }

  .preview-head {
    flex-direction: column;
  }
}
</style>
