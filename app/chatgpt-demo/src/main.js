import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Login from './Login.vue'
import ElementPlus from 'element-plus' //全局引入
import 'element-plus/dist/index.css'

// 定义路由
const routes = [
  { path: '/', component: Login },
  { path: '/app', component: App }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

const app = createApp(App) // 创建应用实例
app.use(ElementPlus)
app.use(router)
app.mount('#app')