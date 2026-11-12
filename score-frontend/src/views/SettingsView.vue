<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTerm,
  getAiSettings,
  getGithubBindUrl,
  getGithubBinding,
  setCurrentTerm,
  unbindGithub,
  updateAiSettings,
  updateGithubConfig,
} from '@/api/settings'
import { useTermStore } from '@/stores/term'
import type { Term } from '@/types'

const route = useRoute()
const termStore = useTermStore()

const aiLoading = ref(false)
const aiSaving = ref(false)
const aiForm = reactive({
  enabled: false,
  baseUrl: '',
  model: '',
  apiKey: '',
  apiKeySet: false,
  apiKeyMasked: '',
  clearApiKey: false,
  ready: false,
})

const termLoading = ref(false)
const termSaving = ref(false)
const newTerm = reactive({ academicYear: '', semester: '1', current: false })

const githubLoading = ref(false)
const githubSaving = ref(false)
const github = reactive({
  enabled: false,
  bound: false,
  login: '',
  clientId: '',
  clientSecret: '',
  clientSecretSet: false,
  clientSecretMasked: '',
  clearClientSecret: false,
  redirectUri: '',
})

onMounted(() => {
  document.title = '设置 · 成绩查询平台'
  loadAiSettings()
  loadGithub()
  termStore.load(true).catch(() => {})
  if (route.query.github === 'bound') ElMessage.success('GitHub 绑定成功')
  else if (route.query.github === 'error') ElMessage.error('GitHub 绑定失败，请重试')
})

async function loadGithub() {
  try {
    const res = await getGithubBinding()
    if (res.data) {
      github.enabled = res.data.enabled
      github.bound = res.data.bound
      github.login = res.data.login || ''
      github.clientId = res.data.clientId || ''
      github.clientSecretSet = res.data.clientSecretSet
      github.clientSecretMasked = res.data.clientSecretMasked || ''
      github.redirectUri = res.data.redirectUri || ''
      github.clientSecret = ''
      github.clearClientSecret = false
    }
  } catch {
    // 设置页其他功能不受影响
  }
}

async function saveGithubConfig() {
  githubSaving.value = true
  try {
    await updateGithubConfig({
      clientId: github.clientId.trim(),
      clientSecret: github.clientSecret.trim() || undefined,
      clearClientSecret: github.clearClientSecret,
    })
    ElMessage.success('GitHub 凭据已保存')
    await loadGithub()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    githubSaving.value = false
  }
}

async function bindGithub() {
  githubLoading.value = true
  try {
    const res = await getGithubBindUrl()
    const url = res.data?.url
    if (!url) {
      ElMessage.error('GitHub 登录尚未配置')
      return
    }
    window.location.href = url
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    githubLoading.value = false
  }
}

async function unbindGithubAccount() {
  try {
    await ElMessageBox.confirm('解除后该 GitHub 账号将无法登录，仍可使用密码登录。', '解除 GitHub 绑定', {
      type: 'warning',
      confirmButtonText: '解除',
      cancelButtonText: '取消',
    })
    await unbindGithub()
    ElMessage.success('已解除绑定')
    await loadGithub()
  } catch {
    // 取消或失败已提示
  }
}

async function loadAiSettings() {
  aiLoading.value = true
  try {
    const res = await getAiSettings()
    const data = res.data
    if (data) {
      aiForm.enabled = data.enabled
      aiForm.baseUrl = data.baseUrl || ''
      aiForm.model = data.model || ''
      aiForm.apiKeySet = data.apiKeySet
      aiForm.apiKeyMasked = data.apiKeyMasked || ''
      aiForm.ready = data.ready
      aiForm.apiKey = ''
      aiForm.clearApiKey = false
    }
  } catch {
    ElMessage.error('AI 设置读取失败')
  } finally {
    aiLoading.value = false
  }
}

async function saveAi() {
  if (aiForm.enabled && (!aiForm.baseUrl.trim() || !aiForm.model.trim())) {
    ElMessage.warning('启用 AI 前请填写接口地址和模型名称')
    return
  }
  aiSaving.value = true
  try {
    await updateAiSettings({
      enabled: aiForm.enabled,
      baseUrl: aiForm.baseUrl.trim(),
      model: aiForm.model.trim(),
      apiKey: aiForm.apiKey.trim() || undefined,
      clearApiKey: aiForm.clearApiKey,
    })
    ElMessage.success('AI 设置已保存')
    await loadAiSettings()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    aiSaving.value = false
  }
}

async function addTerm() {
  if (!newTerm.academicYear.trim()) {
    ElMessage.warning('请填写学年，例如 2026-2027')
    return
  }
  termSaving.value = true
  try {
    await createTerm({
      academicYear: newTerm.academicYear.trim(),
      semester: newTerm.semester,
      current: newTerm.current,
    })
    ElMessage.success('学期已创建')
    newTerm.academicYear = ''
    newTerm.semester = '1'
    newTerm.current = false
    await termStore.load(true)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    termSaving.value = false
  }
}

async function makeCurrent(term: Term) {
  termLoading.value = true
  try {
    await setCurrentTerm(term.id)
    ElMessage.success(`已将 ${term.name} 设为当前学期`)
    await termStore.load(true)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    termLoading.value = false
  }
}
</script>

<template>
  <div class="settings-page">
    <header class="page-head">
      <h1>设置</h1>
      <p>仅管理员可见。这里的修改立即生效，不需要重启服务。</p>
    </header>

    <section class="setting-card" v-loading="aiLoading">
      <div class="card-head">
        <div>
          <h2>AI 表头识别</h2>
          <p>导入 Excel 时由模型建议列映射。只发送表头和脱敏样例，不发送姓名、身份证原文；调用失败自动回退规则识别。</p>
        </div>
        <el-tag :type="aiForm.ready ? 'success' : 'info'" effect="plain">
          {{ aiForm.ready ? '已启用' : '未启用' }}
        </el-tag>
      </div>

      <el-form label-position="top" class="setting-form">
        <el-form-item label="启用 AI 表头识别">
          <el-switch v-model="aiForm.enabled" />
        </el-form-item>
        <el-form-item label="接口地址（OpenAI 兼容）">
          <el-input v-model="aiForm.baseUrl" placeholder="例如 https://api.example.com/v1" clearable />
          <div class="hint">实际请求 {接口地址}/chat/completions</div>
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="aiForm.model" placeholder="例如 gpt-4o-mini" clearable />
        </el-form-item>
        <el-form-item label="API 密钥">
          <el-input
            v-model="aiForm.apiKey"
            type="password"
            show-password
            clearable
            :placeholder="aiForm.apiKeySet ? `已设置（${aiForm.apiKeyMasked}），留空不修改` : '未设置'"
          />
          <el-checkbox v-if="aiForm.apiKeySet" v-model="aiForm.clearApiKey" class="clear-key">
            清除已保存的密钥
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiSaving" @click="saveAi">保存</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="setting-card">
      <div class="card-head">
        <div>
          <h2>GitHub 登录</h2>
          <p>管理员先用密码登录并绑定 GitHub，之后可在登录页使用 GitHub 登录。</p>
        </div>
        <el-tag :type="github.bound ? 'success' : 'info'" effect="plain">
          {{ github.bound ? '已绑定' : '未绑定' }}
        </el-tag>
      </div>
      <el-form label-position="top" class="setting-form">
        <el-form-item label="Client ID">
          <el-input v-model="github.clientId" placeholder="GitHub OAuth App 的 Client ID" clearable />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input
            v-model="github.clientSecret"
            type="password"
            show-password
            clearable
            :placeholder="github.clientSecretSet ? `已设置（${github.clientSecretMasked}），留空不修改` : 'GitHub OAuth App 的 Client Secret'"
          />
          <el-checkbox v-if="github.clientSecretSet" v-model="github.clearClientSecret" class="clear-key">
            清除已保存的 Secret
          </el-checkbox>
        </el-form-item>
        <el-form-item label="回调地址（填到 GitHub OAuth App）">
          <el-input :model-value="github.redirectUri" readonly />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="githubSaving" @click="saveGithubConfig">保存凭据</el-button>
        </el-form-item>
      </el-form>

      <div class="github-row">
        <template v-if="github.bound">
          <span class="github-account">@{{ github.login }}</span>
          <el-button size="small" @click="unbindGithubAccount">解除绑定</el-button>
        </template>
        <template v-else>
          <el-button
            type="primary"
            plain
            :loading="githubLoading"
            :disabled="!github.enabled"
            @click="bindGithub"
          >
            绑定 GitHub
          </el-button>
          <span v-if="!github.enabled" class="hint">
            先在上方填写并保存 Client ID / Client Secret，才能绑定
          </span>
        </template>
      </div>
    </section>

    <section class="setting-card">
      <div class="card-head">
        <div>
          <h2>学年学期</h2>
          <p>学生和成绩按学期分开存放；教师端左上角和导入页都按所选学期操作。</p>
        </div>
      </div>

      <el-table :data="termStore.terms" size="small" class="term-table">
        <el-table-column prop="name" label="学期" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.current" type="success" effect="plain">当前学期</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button
              v-if="!row.current"
              link
              type="primary"
              :loading="termLoading"
              @click="makeCurrent(row)"
            >
              设为当前
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="term-add">
        <el-input v-model="newTerm.academicYear" placeholder="学年，例如 2026-2027" class="term-year" />
        <el-select v-model="newTerm.semester" class="term-semester">
          <el-option label="第一学期" value="1" />
          <el-option label="第二学期" value="2" />
        </el-select>
        <el-checkbox v-model="newTerm.current">设为当前学期</el-checkbox>
        <el-button type="primary" plain :loading="termSaving" @click="addTerm">添加学期</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 28px 20px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--color-text, #111827);
}

.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

.setting-card {
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 10px;
  padding: 20px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.card-head h2 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--color-text, #111827);
}

.card-head p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-secondary, #6b7280);
}

.setting-form {
  max-width: 520px;
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}

.clear-key {
  margin-top: 6px;
}

.term-table {
  margin-bottom: 16px;
}

.term-add {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.term-year {
  width: 200px;
}

.term-semester {
  width: 140px;
}

.muted {
  color: var(--color-text-secondary, #9ca3af);
}

.github-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.github-account {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--color-text, #111827);
}
</style>
