<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-side">
        <h2 class="side-title">欢迎回来</h2>
        <p class="side-desc">登录后继续选购，好物在等你～</p>
        <div class="side-deco">
          <el-icon :size="56" color="#fff"><ShoppingBag /></el-icon>
        </div>
      </div>

      <div class="auth-form">
        <h3 class="form-title">账号登录</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item prop="account">
            <el-input
              v-model="form.account"
              placeholder="请输入账号"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                maxlength="4"
                clearable
              />
              <img
                v-if="captchaImage"
                class="captcha-img"
                :src="captchaImage"
                alt="验证码"
                title="点击刷新"
                @click="refreshCaptcha"
              />
            </div>
          </el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <p class="switch-line">
          还没有账号？
          <router-link
            class="link"
            :to="{ path: '/register', query: route.query }"
            >立即注册</router-link
          >
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { useCartStore } from "@/stores/cart";
import { getCaptcha } from "@/api/auth";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

const formRef = ref();
const loading = ref(false);
const captchaImage = ref("");

const form = reactive({
  account: "",
  password: "",
  captchaId: "",
  captchaCode: "",
});

const rules = {
  account: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  captchaCode: [{ required: true, message: "请输入验证码", trigger: "blur" }],
};

async function refreshCaptcha() {
  try {
    const data = await getCaptcha();
    captchaImage.value = data.image;
    form.captchaId = data.captchaId;
    form.captchaCode = "";
  } catch {
    // 获取失败保持旧图，用户可再次点击重试
  }
}

async function submit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    await userStore.login({ ...form, loginType: "member" });
    cartStore.refresh();
    ElMessage.success("登录成功，欢迎回来～");
    router.push((route.query.redirect as string) || "/");
  } catch {
    // 登录失败时验证码已被服务端作废，刷新一张新码供重试
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
}

onMounted(refreshCaptcha);
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
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
}

/* 左侧品牌区 */
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
  opacity: 0.9;
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

/* 右侧表单 */
.auth-form {
  flex: 1;
  padding: 48px 44px;
  background: #fff;
}

.form-title {
  margin-bottom: 28px;
  font-size: 22px;
  color: var(--text-main);
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  flex: 0 0 120px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  object-fit: cover;
  background: #f5f7fa;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  letter-spacing: 4px;
}

.switch-line {
  margin-top: 20px;
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
    padding: 36px 24px;
  }
}
</style>
