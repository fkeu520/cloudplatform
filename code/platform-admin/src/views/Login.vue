<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>云枢中台</h2>
        <p>通用业务中台基座</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { rsaEncrypt } from '@/api/crypto'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 使用 RSA 公钥加密密码
    const encryptedPassword = await rsaEncrypt(form.password)
    
    const res: any = await request.post('/auth/login', {
      username: form.username,
      password: encryptedPassword
    })
    const token = res.data?.token
    if (!token) {
      ElMessage.error('登录异常，未获取到Token')
      return
    }
    
    localStorage.setItem('token', token)
    localStorage.setItem('username', form.username)
    userStore.setToken(token)
    
    const userInfo = res.data?.user

    // 运营管理员不能登录管理平台
    if (userInfo && userInfo.userType === 2) {
      ElMessage.error('运营管理员请登录运营后台')
      localStorage.removeItem('token')
      return
    }

    if (userInfo) {
      localStorage.setItem('userId', String(userInfo.id || ''))
      userStore.setUserInfo(userInfo)
      userStore.setPermissions(userInfo.perms || [])
    }
    
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // F9: 输出具体错误原因到控制台便于排查（RSA 加密失败等）
    console.error('登录失败:', e)
    const msg = (e instanceof Error) ? e.message : '请检查账号密码'
    ElMessage.error('登录失败，' + msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}
.login-header p {
  color: #999;
  font-size: 14px;
}
.login-form {
  margin-top: 20px;
}
</style>