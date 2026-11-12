import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/student-query',
    name: 'StudentQuery',
    component: () => import('@/views/StudentQuery.vue'),
    meta: { title: '学生成绩查询', requiresAuth: false }
  },
  {
    path: '/teacher',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { title: '教师工作台', requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/teacher/dashboard'
      },
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/TeacherDashboard.vue'),
        meta: { title: '成绩管理' }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'import-data',
        name: 'ImportData',
        component: () => import('@/views/ImportData.vue'),
        meta: { title: '数据导入' }
      },
      {
        path: 'student-management',
        name: 'StudentManagement',
        component: () => import('@/views/StudentManagement.vue'),
        meta: { title: '学生管理' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '设置', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  // 核心修改：加上子目录基路径，和 vite.config 的 base、Nginx 路径完全统一
  history: createWebHistory('/score'),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)

  if (requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (requiresAdmin && !authStore.isAdmin) {
    next('/teacher/dashboard')
  } else if (to.path === '/login' && authStore.isLoggedIn) {
    next('/teacher/dashboard')
  } else {
    next()
  }
})

export default router