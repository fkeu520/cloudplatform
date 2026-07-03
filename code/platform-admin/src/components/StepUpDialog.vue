<template>
  <el-dialog
    v-model="visible"
    title="敏感操作二次验证"
    width="440px"
    :close-on-click-modal="false"
    :show-close="false"
    :before-close="handleCancel"
  >
    <div class="stepup-tip">
      <el-icon style="color:#E6A23C;font-size:20px;margin-right:6px"><Warning /></el-icon>
      即将执行 <b>{{ description }}</b>，请输入登录密码以确认操作。
    </div>
    <el-form ref="formRef" :model="form" @submit.prevent="handleSubmit">
      <el-form-item
        prop="password"
        :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]"
      >
        <el-input
          v-model="form.password"
          type="password"
          placeholder="登录密码"
          show-password
          :prefix-icon="Lock"
          autocomplete="current-password"
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确认 (5 分钟内有效)
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Warning } from '@element-plus/icons-vue'
import { issueStepUp } from '../api/auth'
import { rsaEncrypt } from '../api/crypto'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  /** 操作范围 (如 'tenant:delete' / 'tenant:disable' / 'tenant:admin:reset-pwd') */
  scope: { type: String, required: true },
  /** 用户可读的操作描述 */
  description: { type: String, default: '此操作' },
  /** 验证成功后回调 (拿到 stepUpToken 后执行原操作) */
  onSuccess: { type: Function, required: true },
  /** 是否单次有效 (默认 true) */
  singleUse: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const loading = ref(false)
const formRef = ref()
const form = reactive({ password: '' })

watch(() => props.modelValue, v => (visible.value = v))
watch(visible, v => emit('update:modelValue', v))

function handleCancel() {
  visible.value = false
  form.password = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 1. RSA 加密密码
    const encryptedPassword = await rsaEncrypt(form.password)
    // 2. 调后端 issue step-up token
    const res: any = await issueStepUp({
      password: encryptedPassword,
      scope: props.scope,
      singleUse: props.singleUse
    })
    const stepUpToken: string = res?.data?.stepUpToken
    if (!stepUpToken) {
      throw new Error('未获取到 step-up token')
    }
    ElMessage.success('验证通过, 5 分钟内有效')
    // 3. 回调 (传入 token 供原操作使用)
    await props.onSuccess(stepUpToken)
    // 4. 关闭弹窗 (无论回调成功失败都关)
    visible.value = false
    form.password = ''
  } catch (e: any) {
    // 错误已经在 request 拦截器弹了 ElMessage.error, 这里只关 loading
    form.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.stepup-tip {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 18px;
  background: #fdf6ec;
  border-left: 3px solid #E6A23C;
  color: #5c3c00;
  font-size: 13px;
  border-radius: 4px;
}
.stepup-tip b { color: #c25a00; }
</style>
