# ScoreCheck 成绩管理系统 - 前端

一个可爱风格的 Vue 3 成绩管理系统前端。

## 功能特性

### 学生端
- 📚 成绩查询 - 输入姓名和身份证号查询个人成绩
- 🎨 可爱卡片展示 - 以精美的卡片形式展示各科目成绩
- 📊 成绩统计 - 自动计算总分和平均分

### 教师端
- 🔐 安全登录 - 滑块验证码 + 密码双重验证
- 📋 成绩管理 - 查询、筛选学生成绩数据
- 👥 用户管理 - 新增、编辑、删除教师账户
- 📤 数据导入 - 支持 Excel 批量导入学生和成绩

## 技术栈

- **框架**: Vue 3 + Composition API + TypeScript
- **构建工具**: Vite 5
- **UI 组件库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **图标**: @element-plus/icons-vue

## 项目结构

```
src/
├── api/              # API 接口封装
│   ├── captcha.ts    # 验证码接口
│   ├── auth.ts       # 登录接口
│   ├── score.ts      # 成绩查询接口
│   ├── import.ts     # 数据导入接口
│   └── user.ts       # 用户管理接口
├── assets/
│   └── styles/       # 全局样式
├── components/
│   └── SliderCaptcha.vue  # 滑块验证码组件
├── layouts/
│   └── TeacherLayout.vue  # 教师端布局
├── router/
│   └── index.ts      # 路由配置
├── stores/
│   └── auth.ts       # 认证状态管理
├── types/
│   └── index.ts      # TypeScript 类型定义
├── utils/
│   └── request.ts    # Axios 封装
└── views/
    ├── Login.vue             # 登录页
    ├── StudentQuery.vue      # 学生成绩查询
    ├── TeacherDashboard.vue  # 教师仪表盘
    ├── UserManagement.vue    # 用户管理
    └── ImportData.vue        # 数据导入
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发环境

```bash
npm run dev
```

访问 http://localhost:5173

### 生产构建

```bash
npm run build
```

## 界面设计

### 配色方案

| 颜色 | 用途 |
|------|------|
| `#FF6B9D` | 主色 - 樱花粉 |
| `#7C5CFF` | 辅色 - 梦幻紫 |
| `#FFD93D` | 点缀 - 柠檬黄 |
| `#FFF5F8` | 背景 - 淡粉白 |

### 设计特点

- 🍬 糖果色系设计
- ✨ 丰富的微动画交互
- 📱 响应式布局
- 🎀 圆角卡片风格

## API 对接

项目配置了代理，访问后端 API 时需要确保后端服务运行在 http://localhost:8080

如需修改 API 地址，编辑 `.env` 文件：

```
VITE_API_BASE_URL=http://localhost:8080
```

## 后端接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/captcha/get` | GET | 获取滑块验证码 |
| `/captcha/check` | POST | 校验滑块位置 |
| `/scoreCheck/login` | POST | 教师登录 |
| `/scoreCheck/StudentCheck` | POST | 学生成绩查询 |
| `/scoreCheck/checkList` | POST | 教师成绩列表 |
| `/scoreCheck/importStudent` | POST | 导入学生信息 |
| `/scoreCheck/importScore` | POST | 导入成绩 |
| `/scoreCheck/creatAdmin` | POST | 创建用户 |
| `/scoreCheck/getUserList` | GET | 获取用户列表 |
| `/scoreCheck/updateUser` | POST | 更新用户 |
| `/scoreCheck/deleteUser` | GET | 删除用户 |

## 许可证

MIT License