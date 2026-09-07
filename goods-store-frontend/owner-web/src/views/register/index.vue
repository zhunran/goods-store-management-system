<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-side">
        <h2 class="side-title">加入我们</h2>
        <p class="side-desc">注册即享便捷购物体验<br />好物不贵，优选生活</p>
        <div class="side-deco">
          <el-icon :size="56" color="#fff"><Present /></el-icon>
        </div>
      </div>

      <div class="auth-form">
        <h3 class="form-title">创建账号</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          label-position="top"
        >
          <el-form-item prop="account" label="账号">
            <el-input
              v-model="form.account"
              placeholder="3-32 位字符"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="6-32 位字符"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-form-item prop="confirm" label="确认密码">
            <el-input
              v-model="form.confirm"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-form-item prop="phone" label="手机号">
            <el-input
              v-model="form.phone"
              placeholder="选填，用于联系"
              :prefix-icon="Iphone"
              clearable
            />
          </el-form-item>
          <el-form-item prop="email" label="邮箱">
            <el-input
              v-model="form.email"
              placeholder="选填"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="submit"
          >
            注 册
          </el-button>
        </el-form>

        <p class="switch-line">
          已有账号？
          <router-link class="link" :to="{ path: '/login', query: route.query }"
            >直接登录</router-link
          >
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock, Iphone, Message } from "@element-plus/icons-vue";
import { register } from "@/api/auth";
import type { RegisterRequest } from "@/api/types";

const route = useRoute();
const router = useRouter();

const formRef = ref();
const loading = ref(false);

const form = reactive<RegisterRequest & { confirm: string }>({
  account: "",
  password: "",
  confirm: "",
  phone: "",
  email: "",
});

const rules = {
  account: [
    { required: true, message: "请输入账号", trigger: "blur" },
    { min: 3, max: 32, message: "账号长度 3-32 位", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 32, message: "密码长度 6-32 位", trigger: "blur" },
  ],
  confirm: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    {
      validator: (_: unknown, value: string, callback: (e?: Error) => void) => {
        if (value !== form.password)
          callback(new Error("两次输入的密码不一致"));
        else callback();
      },
      trigger: "blur",
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "手机号格式不正确",
      trigger: "blur",
    },
  ],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
};

async function submit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const { confirm: _confirm, ...data } = form;
    await register(data);
    ElMessage.success("注册成功，快去登录吧～");
    router.push({ path: "/login", query: route.query });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(
      circle at 15% 20%,
      rgba(37, 99, 235, 0.12),
      transparent 40%
    ),
    radial-gradient(
      circle at 85% 80%,
      rgba(129, 140, 248, 0.1),
      transparent 40%
    ),
    var(--bg);
  padding: 24px;
}

.auth-card {
  display: flex;
  width: 760px;
  max-width: 100%;
  max-height: 92vh;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
}

.auth-side {
  flex: 0 0 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 16px;
  padding: 48px 36px;
  color: #fff;
  background: var(--primary-gradient);
}

.side-title {
  font-size: 28px;
  font-weight: 800;
}

.side-desc {
  font-size: 14px;
  opacity: 0.92;
  line-height: 1.8;
}

.side-deco {
  margin-top: 24px;
  width: 96px;
  height: 96px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.auth-form {
  flex: 1;
  padding: 36px 44px;
  background: #fff;
  overflow-y: auto;
}

.form-title {
  margin-bottom: 20px;
  font-size: 22px;
  color: var(--text-main);
}

:deep(.el-form-item__label) {
  padding-bottom: 4px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  letter-spacing: 4px;
}

.switch-line {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: var(--text-sub);
}

.link {
  color: var(--primary);
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .auth-side {
    display: none;
  }

  .auth-form {
    padding: 28px 20px;
  }
}
</style>
