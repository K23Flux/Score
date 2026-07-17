<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  PhSquaresFour,
  PhUsers,
  PhUploadSimple,
  PhUserCircle,
  PhSignOut,
  PhList,
  PhMoon,
  PhSun,
} from '@phosphor-icons/vue'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useTheme()

const activeMenu = computed(() => route.path)
const mobileMenuOpen = ref(false)
const currentTime = ref('')

interface MenuItem {
  path: string
  name: string
  icon: any
  shortcut: string
}

const menuItems: MenuItem[] = [
  { path: '/teacher/dashboard', name: 'DASHBOARD', icon: PhSquaresFour, shortcut: 'D' },
  { path: '/teacher/student-management', name: 'STUDENTS', icon: PhUserCircle, shortcut: 'S' },
  { path: '/teacher/import-data', name: 'IMPORT', icon: PhUploadSimple, shortcut: 'I' },
  { path: '/teacher/user-management', name: 'USERS', icon: PhUsers, shortcut: 'U' },
]

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('CONFIRM LOGOUT?', 'SYSTEM', {
      confirmButtonText: 'LOGOUT',
      cancelButtonText: 'CANCEL',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    authStore.logout()
    ElMessage.success('SESSION TERMINATED')
    router.push('/login')
  } catch {}
}

// Clock
const updateClock = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${h}:${m}:${s}`
}

let clockTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<template>
  <div class="scorecheck-app">
    <!-- Top Terminal Bar -->
    <header class="top-bar">
      <!-- Left: Brand -->
      <div class="top-bar-left">
        <span class="brand-prefix"><span class="amber-dot" /></span>
        <RouterLink to="/teacher/dashboard" class="brand-name">ScoreCheck</RouterLink>
        <span class="brand-sep">&gt;&gt;</span>
      </div>

      <!-- Center: Nav -->
      <nav class="top-bar-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: activeMenu === item.path }"
        >
          <component :is="item.icon" size="14" weight="regular" />
          <span class="nav-label">{{ item.name }}</span>
          <span class="nav-shortcut">{{ item.shortcut }}</span>
        </RouterLink>
      </nav>

      <!-- Right: Meta -->
      <div class="top-bar-right">
        <span class="clock">{{ currentTime }}</span>
        <span class="sep">|</span>
        <button class="theme-toggle" @click="toggleTheme" :aria-label="isDark ? 'Switch to light' : 'Switch to dark'">
          <PhSun v-if="isDark" size="14" weight="bold" />
          <PhMoon v-else size="14" weight="bold" />
        </button>
        <span class="sep">|</span>
        <div class="user-pill">
          <span class="user-name">{{ authStore.userName || 'GUEST' }}</span>
          <span class="user-class">{{ authStore.className || '' }}</span>
        </div>
        <button class="logout-btn" @click="handleLogout">
          <PhSignOut size="14" weight="bold" />
        </button>
      </div>
    </header>

    <!-- Mobile hamburger -->
    <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">
      <PhList weight="bold" size="20" />
    </button>

    <!-- Mobile slide-out menu -->
    <Teleport to="body">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    </Teleport>
    <Teleport to="body">
      <div v-if="mobileMenuOpen" class="mobile-sheet">
        <div class="sheet-header">
          <span class="sheet-brand">ScoreCheck</span>
          <button class="sheet-close" @click="mobileMenuOpen = false">&times;</button>
        </div>
        <nav class="sheet-nav">
          <RouterLink
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="sheet-item"
            :class="{ active: activeMenu === item.path }"
            @click="mobileMenuOpen = false"
          >
            <component :is="item.icon" size="16" weight="regular" />
            <span>{{ item.name }}</span>
          </RouterLink>
        </nav>
        <div class="sheet-footer">
          <button class="sheet-logout" @click="handleLogout">
            <PhSignOut size="14" weight="bold" />
            LOGOUT
          </button>
        </div>
      </div>
    </Teleport>

    <!-- Main Content -->
    <main class="content-area">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.scorecheck-app {
  min-height: 100vh;
  background: var(--bg);
  display: flex;
  flex-direction: column;
}

/* --- Top Terminal Bar --- */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 40px;
  background: var(--bg-deep);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 20px;
  font-size: 12px;
  white-space: nowrap;
}

/* Left: Brand */
.top-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.amber-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--amber);
  animation: breathe 3s ease-in-out infinite;
  margin-right: 2px;
}

.brand-name {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.05em;
  text-decoration: none;
  transition: color var(--fast) var(--ease-out);
}

.brand-name:hover {
  color: var(--amber);
}

.brand-sep {
  color: var(--text-muted);
  font-size: 10px;
}

/* Center: Nav */
.top-bar-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  justify-content: center;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.06em;
  transition: all var(--fast) var(--ease-out);
  position: relative;
  white-space: nowrap;
}

.nav-link:hover {
  color: var(--text-secondary);
}

.nav-link.active {
  color: var(--amber);
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -13px;
  left: 10px;
  right: 10px;
  height: 1px;
  background: var(--amber);
}

.nav-label {
  font-family: 'IBM Plex Mono', monospace;
}

.nav-shortcut {
  font-size: 9px;
  color: var(--text-muted);
  opacity: 0.5;
  margin-left: 2px;
}

.nav-link.active .nav-shortcut {
  opacity: 0.8;
}

/* Right: Meta */
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.clock {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.sep {
  color: var(--border-strong);
  font-size: 10px;
}

.theme-toggle {
  width: 26px;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.theme-toggle:hover {
  border-color: var(--amber);
  color: var(--amber);
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
}

.user-class {
  font-size: 10px;
  color: var(--text-muted);
}

.logout-btn {
  width: 26px;
  height: 26px;
  border: 1px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.logout-btn:hover {
  border-color: var(--red);
  color: var(--red);
  background: var(--red-bg);
}

/* --- Mobile Menu Button --- */
.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 8px;
  left: 12px;
  z-index: 200;
  width: 36px;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: var(--bg-deep);
  color: var(--text-secondary);
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* --- Mobile Overlay --- */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 150;
}

/* --- Mobile Sheet --- */
.mobile-sheet {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 260px;
  background: var(--surface);
  border-left: 1px solid var(--border);
  z-index: 200;
  display: flex;
  flex-direction: column;
  padding: 56px 0 0;
  animation: slideIn 0.25s var(--ease-out);
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to   { transform: translateX(0); }
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.sheet-brand {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.sheet-close {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sheet-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 0;
  flex: 1;
}

.sheet-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  transition: all var(--fast) var(--ease-out);
  font-family: 'IBM Plex Mono', monospace;
}

.sheet-item:hover {
  background: var(--surface-raised);
  color: var(--text-primary);
}

.sheet-item.active {
  color: var(--amber);
  background: var(--amber-bg);
  border-left: 2px solid var(--amber);
}

.sheet-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}

.sheet-logout {
  width: 100%;
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  color: var(--text-muted);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all var(--fast) var(--ease-out);
}

.sheet-logout:hover {
  border-color: var(--red);
  color: var(--red);
  background: var(--red-bg);
}

/* --- Content --- */
.content-area {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* --- Responsive --- */
@media (max-width: 1024px) {
  .top-bar-nav {
    display: none;
  }
}

@media (max-width: 768px) {
  .top-bar {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .content-area {
    padding: 16px;
    padding-top: 56px;
  }
}
</style>
