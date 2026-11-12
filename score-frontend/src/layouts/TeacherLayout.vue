<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  PhGear,
  PhList,
  PhSquaresFour,
  PhUploadSimple,
  PhUserCircle,
  PhUsers,
} from '@phosphor-icons/vue'
import { getMe } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useTermStore } from '@/stores/term'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const termStore = useTermStore()

const mobileMenuOpen = ref(false)
const menuButton = ref<HTMLButtonElement | null>(null)
const sidebar = ref<HTMLElement | null>(null)

interface MenuItem {
  path: string
  name: string
  icon: Component
}

const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
    { path: '/teacher/dashboard', name: '成绩管理', icon: PhSquaresFour },
    { path: '/teacher/student-management', name: '学生管理', icon: PhUserCircle },
    { path: '/teacher/import-data', name: '数据导入', icon: PhUploadSimple },
    { path: '/teacher/user-management', name: '用户管理', icon: PhUsers },
  ]
  if (authStore.isAdmin) {
    items.push({ path: '/teacher/settings', name: '设置', icon: PhGear })
  }
  return items
})

const termValue = computed({
  get: () => (termStore.selected ? termStore.termKey(termStore.selected) : ''),
  set: (value: string) => termStore.selectByKey(value),
})

const activeTitle = computed(() => {
  const item = menuItems.value.find((entry) => entry.path === route.path)
  return item?.name || '教师端'
})

function toggleMobileMenu() {
  if (mobileMenuOpen.value) closeMobileMenu()
  else openMobileMenu()
}

function openMobileMenu() {
  mobileMenuOpen.value = true
  nextTick(() => {
    const firstLink = sidebar.value?.querySelector<HTMLElement>('.side-link')
    firstLink?.focus()
  })
}

function closeMobileMenu(returnFocus = true) {
  if (!mobileMenuOpen.value) return
  mobileMenuOpen.value = false
  if (returnFocus) menuButton.value?.focus()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {}
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && mobileMenuOpen.value) closeMobileMenu()
}

function handleResize() {
  if (window.innerWidth >= 1024) closeMobileMenu(false)
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize)
  termStore.load().catch(() => {})
  // 角色以数据库为准，刷新页面后同步一次
  getMe()
    .then((res) => {
      if (res.data) {
        authStore.setUserInfo(res.data.userName, res.data.className, res.data.role)
      }
    })
    .catch(() => {})
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="app-shell">
    <aside ref="sidebar" class="sidebar" :class="{ 'is-open': mobileMenuOpen }" aria-label="教师端导航">
      <div class="sidebar-brand">
        <strong>成绩查询平台</strong>
        <span>Score · 教师端</span>
      </div>

      <div class="side-term">
        <label for="term-select">学年学期</label>
        <el-select
          id="term-select"
          v-model="termValue"
          size="small"
          class="term-select"
          placeholder="选择学期"
        >
          <el-option
            v-for="term in termStore.terms"
            :key="term.id"
            :label="term.name"
            :value="termStore.termKey(term)"
          />
        </el-select>
      </div>

      <nav class="side-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="side-link"
          :class="{ active: route.path === item.path }"
          @click="closeMobileMenu(false)"
        >
          <component :is="item.icon" size="16" weight="regular" class="nav-icon" />
          <span>{{ item.name }}</span>
        </RouterLink>
      </nav>

      <div class="side-foot">
        <div class="side-user">
          <b>{{ authStore.userName || '教师' }}</b>
          <span v-if="authStore.className">{{ authStore.className }}</span>
        </div>
        <button class="logout-btn" type="button" @click="handleLogout">退出</button>
      </div>
    </aside>

    <div v-if="mobileMenuOpen" class="drawer-overlay" @click="closeMobileMenu()" />

    <header class="mobile-bar">
      <button
        ref="menuButton"
        class="icon-btn"
        type="button"
        aria-label="打开导航"
        :aria-expanded="mobileMenuOpen"
        @click="toggleMobileMenu"
      >
        <PhList size="20" weight="bold" />
      </button>
      <span class="mobile-title">{{ activeTitle }}</span>
      <span class="mobile-user">{{ authStore.userName || '教师' }}</span>
    </header>

    <main class="content-area">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--color-bg);
}

.sidebar {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 130;
  width: 216px;
  padding: 20px 12px 16px;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
}

.sidebar-brand {
  padding: 4px 12px 20px;
}

.sidebar-brand strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.side-term {
  padding: 0 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-term label {
  font-size: 11px;
  color: var(--color-text-secondary, #6b7280);
}

.term-select {
  width: 100%;
}

.sidebar-brand span {
  display: block;
  font-size: 11px;
  color: var(--color-text-muted);
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-control);
  color: var(--color-text-secondary);
  font-size: 14px;
  text-decoration: none;
  transition: background-color .15s ease, color .15s ease;
}

.side-link:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.side-link.active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-icon {
  flex-shrink: 0;
}

.side-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 12px 0;
  border-top: 1px solid var(--color-border);
}

.side-user {
  flex: 1;
  min-width: 0;
}

.side-user b {
  display: block;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.side-user span {
  display: block;
  font-size: 12px;
  color: var(--color-text-muted);
}

.logout-btn {
  height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.logout-btn:hover {
  background: var(--color-surface-muted);
  color: var(--color-text);
}

.content-area {
  margin-left: 216px;
  max-width: 1400px;
  min-width: 0;
  padding: 28px 32px 48px;
}

.mobile-bar {
  display: none;
}

.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(16, 24, 40, .35);
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: #fff;
  color: var(--color-text-secondary);
}

.icon-btn:hover {
  border-color: var(--color-border-strong);
  color: var(--color-text);
}

@media (max-width: 1023px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform .18s ease;
  }

  .sidebar.is-open {
    transform: none;
  }

  .content-area {
    margin-left: 0;
    max-width: none;
    padding: 20px 24px 40px;
  }

  .mobile-bar {
    position: sticky;
    top: 0;
    z-index: 80;
    display: flex;
    align-items: center;
    gap: 12px;
    height: 56px;
    padding: 0 16px;
    background: #fff;
    border-bottom: 1px solid var(--color-border);
  }

  .mobile-title {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .mobile-user {
    font-size: 12px;
    color: var(--color-text-muted);
    white-space: nowrap;
  }
}

@media (max-width: 767px) {
  .content-area {
    padding: 16px;
  }

  .mobile-user {
    display: none;
  }
}
</style>
