<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">商城管理后台</div>
      <div class="login-subtitle">Goods Store Admin</div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="onSubmit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="管理员账号" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onSubmit">
          登 录
        </el-button>
      </el-form>
      <div class="login-tip">默认账号：admin / 123456</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ account: form.account, password: form.password, loginType: 'admin' })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 错误提示已在拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
}
.login-card {
  width: 380px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
.login-title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
  color: #111827;
}
.login-subtitle {
  text-align: center;
  color: #9ca3af;
  margin: 6px 0 28px;
  letter-spacing: 1px;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}
.login-tip {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}
</style>
