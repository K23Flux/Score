<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { PhLock, PhUser } from '@phosphor-icons/vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showCaptcha = ref(false)
const captchaToken = ref('')

const formData = reactive({ userName: '', password: '' })

const rules: FormRules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (loading.value) return

  formRef.value?.clearValidate()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!captchaToken.value) {
    showCaptcha.value = true
    return
  }

  loading.value = true
  try {
    const res = await login({
      userName: formData.userName.trim(),
      password: formData.password,
      captchaId: captchaToken.value,
    })
    const token = res.data?.token
    if (token) {
      authStore.setToken(token)
      authStore.setUserInfo(formData.userName.trim(), '')
      ElMessage.success('登录成功')
      router.push('/teacher/dashboard')
    }
  } catch {
    captchaToken.value = ''
  } finally {
    loading.value = false
  }
}

const handleCaptchaSuccess = (token: string) => {
  captchaToken.value = token
  handleLogin()
}

const goToStudentQuery = () => router.push('/student-query')

onMounted(() => {
  document.title = '教师登录 · 成绩查询平台'
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand-line">
        <strong>成绩查询平台</strong>
        <span>Score</span>
      </div>

      <h1>教师登录</h1>
      <p class="page-desc">登录后管理学生与成绩数据</p>

      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="formData.userName"
            :prefix-icon="PhUser"
            autocomplete="username"
            placeholder="请输入用户名"
            @keydown.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            :prefix-icon="PhLock"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
            @keydown.enter="handleLogin"
          />
        </el-form-item>

        <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <div class="login-foot">
        <button class="link-btn" type="button" @click="goToStudentQuery">学生查分</button>
      </div>
    </div>

    <SliderCaptcha v-model="showCaptcha" @success="handleCaptchaSuccess" />
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 24px;
  background: var(--color-bg);
}

.login-card {
  width: min(400px, 100%);
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

.login-submit {
  width: 100%;
  height: 40px;
  margin-top: 4px;
}

.login-foot {
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

@media (max-width: 767px) {
  .login-page {
    padding: 24px 16px;
  }

  .login-card {
    padding: 24px;
  }

  h1 {
    font-size: 23px;
  }
}
</style>
