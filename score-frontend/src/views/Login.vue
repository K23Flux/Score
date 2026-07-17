<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { PhUser, PhLock, PhArrowRight, PhMonitor, PhDatabase } from '@phosphor-icons/vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)
const showCaptcha = ref(false)
const captchaToken = ref('')
const bootPhase = ref(0)
const bootComplete = ref(false)
const cursorBlink = ref(true)

const formData = reactive({ userName: '', password: '' })
const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const bootChecks = [
  { key: 'display', label: 'INITIALIZING DISPLAY...', done: false },
  { key: 'network', label: 'CHECKING NETWORK...', done: false },
  { key: 'database', label: 'CONNECTING TO DATABASE...', done: false },
  { key: 'auth', label: 'LOADING AUTH MODULE...', done: false },
  { key: 'ready', label: 'SYSTEM READY', done: false },
]

// Cursor blink interval
let cursorTimer: ReturnType<typeof setInterval> | null = null

// Boot sequence
const startBootSequence = () => {
  const delays = [400, 900, 1500, 2100, 2600]
  delays.forEach((delay, i) => {
    setTimeout(() => {
      bootChecks[i].done = true
      bootPhase.value = i + 1
    }, delay)
  })
  setTimeout(() => {
    bootComplete.value = true
  }, 3000)
}

// Cursor blink
const startCursor = () => {
  cursorTimer = setInterval(() => {
    cursorBlink.value = !cursorBlink.value
  }, 530)
}

const stopCursor = () => {
  if (cursorTimer) clearInterval(cursorTimer)
}

const handleLogin = async () => {
  if (!bootComplete.value) return
  if (!captchaToken.value) { showCaptcha.value = true; return }
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await login({ userName: formData.userName, password: formData.password, captchaId: captchaToken.value })
      const token = res.data?.token
      if (token) {
        authStore.setToken(token)
        authStore.setUserInfo(formData.userName, '')
        ElMessage.success('AUTH SUCCESSFUL')
        router.push('/teacher/dashboard')
      }
    } catch {
      captchaToken.value = ''
    }
    finally { loading.value = false }
  })
}

const goToStudentQuery = () => { router.push('/student-query') }

// Clock
const updateClock = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  const el = document.getElementById('clock')
  if (el) el.textContent = `${h}:${m}:${s}`
}

let clockTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  document.title = 'ScoreCheck — System Boot'
  startBootSequence()
  startCursor()
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onBeforeUnmount(() => {
  stopCursor()
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<template>
  <div class="scorecheck-login">
    <!-- CRT scanline overlay -->
    <div class="scanline-overlay" />

    <!-- Background grid -->
    <div class="bg-grid" />

    <!-- Top status bar -->
    <header class="status-bar">
      <span class="status-brand">ScoreCheck</span>
      <span class="status-time" id="clock"></span>
    </header>

    <div class="login-body">
      <!-- Boot sequence -->
      <div class="boot-panel" :class="{ 'boot-hidden': bootComplete }">
        <div class="boot-header">
          <span class="boot-label">// SYSTEM BOOT SEQUENCE</span>
        </div>
        <div class="boot-list">
          <div
            v-for="check in bootChecks"
            :key="check.key"
            class="boot-row"
            :class="{ done: check.done }"
          >
            <span class="boot-icon" :class="{ ok: check.done }">
              <template v-if="check.done">&#10003;</template>
              <template v-else>&#9644;</template>
            </span>
            <span class="boot-text">{{ check.label }}</span>
            <span v-if="!check.done" class="boot-cursor">
              <span class="cursor-block" :class="{ visible: cursorBlink }">&nbsp;</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Login form (appears after boot) -->
      <div v-show="bootComplete" class="form-panel animate-fade-in-up">
        <div class="form-header">
          <h2 class="form-title">USER AUTHENTICATION</h2>
          <div class="form-line" />
        </div>

        <el-form ref="formRef" :model="formData" :rules="rules" class="login-form" @submit.prevent>
          <div class="form-field">
            <label class="field-label">USERNAME</label>
            <div class="input-wrapper">
              <PhUser size="16" weight="duotone" class="field-icon" />
              <el-input
                v-model="formData.userName"
                placeholder=""
                size="large"
                @keydown.enter="handleLogin"
              />
            </div>
          </div>

          <div class="form-field">
            <label class="field-label">PASSWORD</label>
            <div class="input-wrapper">
              <PhLock size="16" weight="duotone" class="field-icon" />
              <el-input
                v-model="formData.password"
                type="password"
                placeholder=""
                size="large"
                show-password
                @keydown.enter="handleLogin"
              />
            </div>
          </div>

          <button
            type="button"
            class="login-submit"
            :disabled="loading || !bootComplete"
            @click="handleLogin"
          >
            <span class="submit-text">
              <template v-if="loading">AUTHENTICATING&#8203;...</template>
              <template v-else>LOGIN &#8594;</template>
            </span>
          </button>
        </el-form>

        <div class="form-footer">
          <div class="footer-line" />
          <button class="student-link" @click="goToStudentQuery">
            <span class="link-icon">&#9656;</span>
            STUDENT QUERY PORTAL
          </button>
        </div>
      </div>
    </div>

    <SliderCaptcha v-model="showCaptcha" @success="(token: string) => { captchaToken = token; handleLogin() }" />
  </div>
</template>

<style scoped>
.scorecheck-login {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  overflow: hidden;
  position: relative;
}

/* --- Background Grid --- */
.bg-grid {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(var(--border) 1px, transparent 1px),
    linear-gradient(90deg, var(--border) 1px, transparent 1px);
  background-size: 48px 48px;
  opacity: 0.3;
  pointer-events: none;
}

/* --- Status Bar --- */
.status-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 32px;
  background: var(--bg-deep);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.08em;
}

.status-brand {
  color: var(--amber);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.06em;
}

/* --- Body --- */
.login-body {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  gap: 48px;
}

/* --- Boot Panel --- */
.boot-panel {
  max-width: 380px;
  width: 100%;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.boot-panel.boot-hidden {
  opacity: 0;
  transform: translateX(-20px);
  pointer-events: none;
  max-width: 0;
  padding: 0;
  overflow: hidden;
  margin: 0;
}

.boot-header {
  margin-bottom: 16px;
}

.boot-label {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.boot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.boot-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 4px 0;
  transition: color var(--fast) var(--ease-out);
}

.boot-row.done {
  color: var(--emerald);
}

.boot-icon {
  font-size: 10px;
  width: 14px;
  text-align: center;
  flex-shrink: 0;
}

.boot-icon.ok {
  color: var(--emerald);
}

.boot-text {
  flex: 1;
  letter-spacing: 0.04em;
}

.boot-cursor .cursor-block {
  display: inline-block;
  width: 8px;
  height: 14px;
  background: var(--amber);
  opacity: 0;
  transition: opacity 100ms;
}

.boot-cursor .cursor-block.visible {
  opacity: 1;
}

/* --- Form Panel --- */
.form-panel {
  max-width: 380px;
  width: 100%;
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 28px 24px 24px;
  opacity: 0;
  animation: fadeInUp 0.5s var(--ease-out) 0s forwards;
}

.form-header {
  margin-bottom: 24px;
}

.form-title {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.15em;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.form-line {
  height: 1px;
  background: var(--amber);
  opacity: 0.4;
}

/* --- Form Fields --- */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper :deep(.el-input__wrapper) {
  padding-left: 40px;
  background: var(--bg-deep);
}

.input-wrapper :deep(.el-input__inner) {
  padding-left: 38px;
}

.field-icon {
  position: absolute;
  left: 14px;
  color: var(--text-muted);
  z-index: 1;
  pointer-events: none;
}

/* --- Submit Button --- */
.login-submit {
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
  margin-top: 4px;
}

.login-submit:hover:not(:disabled) {
  background: var(--amber-dim);
  box-shadow: 0 6px 24px rgba(240, 165, 0, 0.3);
  transform: translateY(-1px);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.login-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.submit-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* --- Footer --- */
.form-footer {
  margin-top: 24px;
}

.footer-line {
  height: 1px;
  background: var(--border);
  margin-bottom: 16px;
}

.student-link {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  padding: 10px 16px;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
  width: 100%;
  justify-content: center;
}

.student-link:hover {
  border-color: var(--amber);
  color: var(--amber);
  background: var(--amber-bg);
}

.link-icon {
  font-size: 10px;
}

/* --- Responsive: mobile stack --- */
@media (max-width: 768px) {
  .login-body {
    flex-direction: column;
    padding: 48px 16px;
    gap: 32px;
  }

  .boot-panel {
    display: none;
  }

  .form-panel {
    animation: none;
    opacity: 1;
    background: var(--surface);
    border: 1px solid var(--border);
    padding: 28px 24px 24px;
  }
}

/* --- Pulse dot for status bar --- */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--emerald);
  margin-right: 6px;
  animation: breathe 3s ease-in-out infinite;
}
</style>
