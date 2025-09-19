<template>
  <div class="auth-container">
    <div class="welcome-section">
      <div class="welcome-content">
        <div class="calendar-icon">📅</div>
        <h1 class="welcome-title">智能日程助手</h1>
        <p class="welcome-subtitle">一站式智能日程管理平台，帮助您高效安排时间，避免日程冲突，提升工作效率。</p>

        <div class="features">
          <div class="feature-item">
            <span class="feature-icon">✨</span>
            <span>智能日程安排与冲突检测</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔔</span>
            <span>实时提醒与通知</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <span>可视化日程管理</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔒</span>
            <span>安全可靠的用户数据保护</span>
          </div>
        </div>
      </div>
    </div>

    <div class="form-section">
      <div class="form-header">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">请登录或注册您的账户</p>
      </div>

      <div class="form-container">
        <div class="form-tabs">
          <div class="tab" :class="{ active: isLogin }" @click="switchToLogin">登录</div>
          <div class="tab" :class="{ active: !isLogin }" @click="switchToRegister">注册</div>
        </div>

        <form v-if="isLogin" @submit.prevent="login">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
                type="text"
                class="form-input"
                v-model="loginForm.username"
                placeholder="请输入用户名"
                required
            >
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input
                type="password"
                class="form-input"
                v-model="loginForm.password"
                placeholder="请输入密码"
                required
            >
          </div>

          <div v-if="loginError" class="error-message">{{ loginError }}</div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <form v-else @submit.prevent="register">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
                type="text"
                class="form-input"
                v-model="registerForm.username"
                placeholder="请输入用户名 (3-20个字符)"
                required
                minlength="3"
                maxlength="20"
            >
          </div>

          <div class="form-group">
            <label class="form-label">邮箱</label>
            <input
                type="email"
                class="form-input"
                v-model="registerForm.email"
                placeholder="请输入邮箱地址"
                required
            >
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input
                type="password"
                class="form-input"
                v-model="registerForm.password"
                placeholder="请输入密码 (6-40个字符)"
                required
                minlength="6"
                maxlength="40"
            >
          </div>

          <div v-if="registerError" class="error-message">{{ registerError }}</div>
          <div v-if="registerSuccess" class="success-message">{{ registerSuccess }}</div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    
    const isLogin = ref(true)
    const loading = ref(false)
    
    const loginForm = reactive({
      username: '',
      password: ''
    })
    
    const registerForm = reactive({
      username: '',
      email: '',
      password: ''
    })
    
    const loginError = ref('')
    const registerError = ref('')
    const registerSuccess = ref('')

    const switchToLogin = () => {
      isLogin.value = true
      loginError.value = ''
    }

    const switchToRegister = () => {
      isLogin.value = false
      registerError.value = ''
      registerSuccess.value = ''
    }

    const login = async () => {
      if (!loginForm.username || !loginForm.password) {
        loginError.value = '请填写所有字段'
        return
      }

      loading.value = true
      loginError.value = ''

      try {
        // 设置基础URL以确保请求发送到正确的地址
        axios.defaults.baseURL = 'http://localhost:8080';
        
        const response = await axios.post('/api/auth/signin', {
          username: loginForm.username,
          password: loginForm.password
        })

        // 保存token到localStorage
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data));

        // 显示成功消息
        ElMessage.success('登录成功')

        // 跳转到主页
        router.push('/app')
      } catch (error) {
        loginError.value = error.response?.data?.message || '登录失败，请检查用户名和密码'
        ElMessage.error(loginError.value)
      } finally {
        loading.value = false
      }
    }

    const register = async () => {
      if (!registerForm.username || !registerForm.email || !registerForm.password) {
        registerError.value = '请填写所有字段'
        return
      }

      if (registerForm.username.length < 3 || registerForm.username.length > 20) {
        registerError.value = '用户名长度应在3-20个字符之间'
        return
      }

      if (registerForm.password.length < 6 || registerForm.password.length > 40) {
        registerError.value = '密码长度应在6-40个字符之间'
        return
      }

      loading.value = true
      registerError.value = ''
      registerSuccess.value = ''

      try {
        // 设置基础URL以确保请求发送到正确的地址
        axios.defaults.baseURL = 'http://localhost:8080';
        
        const response = await axios.post('/api/auth/signup', {
          username: registerForm.username,
          email: registerForm.email,
          password: registerForm.password
        })

        registerSuccess.value = response.data.message
        ElMessage.success('注册成功')

        // 清空表单
        registerForm.username = ''
        registerForm.email = ''
        registerForm.password = ''

        // 自动切换到登录
        setTimeout(() => {
          isLogin.value = true
          registerSuccess.value = ''
        }, 2000)
      } catch (error) {
        registerError.value = error.response?.data?.message || '注册失败'
        ElMessage.error(registerError.value)
      } finally {
        loading.value = false
      }
    }
    
    return {
      isLogin,
      loading,
      loginForm,
      registerForm,
      loginError,
      registerError,
      registerSuccess,
      switchToLogin,
      switchToRegister,
      login,
      register
    }
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.auth-container {
  display: flex;
  width: 900px;
  height: 550px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  margin: auto;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.welcome-section {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 50px 30px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.welcome-section::before {
  content: "";
  position: absolute;
  top: -50px;
  right: -50px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.welcome-section::after {
  content: "";
  position: absolute;
  bottom: -80px;
  left: -30px;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

.welcome-content {
  position: relative;
  z-index: 2;
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 20px;
}

.welcome-subtitle {
  font-size: 16px;
  opacity: 0.9;
  line-height: 1.6;
  margin-bottom: 30px;
}

.features {
  margin-top: 30px;
}

.feature-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.feature-icon {
  margin-right: 10px;
  font-size: 20px;
}

.form-section {
  flex: 1;
  padding: 50px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.form-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.form-subtitle {
  color: #666;
  font-size: 14px;
}

.form-container {
  width: 100%;
}

.form-tabs {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 15px 0;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  color: #999;
  transition: all 0.3s;
}

.tab.active {
  color: #667eea;
  border-bottom: 3px solid #667eea;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 15px;
  border: 2px solid #e1e1e1;
  border-radius: 10px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-input:focus {
  border-color: #667eea;
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.submit-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-message {
  color: #e74c3c;
  font-size: 14px;
  margin-top: 5px;
  display: block;
}

.success-message {
  color: #27ae60;
  font-size: 14px;
  margin-top: 5px;
  display: block;
}

.calendar-icon {
  font-size: 60px;
  text-align: center;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .auth-container {
    flex-direction: column;
    width: 90%;
    height: auto;
  }

  .welcome-section {
    padding: 30px 20px;
  }
}
</style>