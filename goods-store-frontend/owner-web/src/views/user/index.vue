<template>
  <div class="user-page content-wrap">
    <div class="user-layout">
      <!-- 左侧个人卡 -->
      <aside class="profile-card card">
        <el-avatar :size="72" class="avatar">
          {{ avatarText }}
        </el-avatar>
        <h3 class="profile-name">{{ profile?.name || '亲爱的会员' }}</h3>
        <p class="profile-account">{{ profile?.account }}</p>
        <p v-if="profile?.createdTime" class="profile-join">
          {{ profile.createdTime.slice(0, 10) }} 加入丰峦优选
        </p>
        <el-menu :default-active="activePanel" class="user-menu" @select="onSelect">
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>我的资料
          </el-menu-item>
          <el-menu-item index="address">
            <el-icon><Location /></el-icon>收货地址
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>修改密码
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧内容 -->
      <div class="panel-area">
        <!-- 我的资料 -->
        <div v-if="activePanel === 'profile'" class="panel card">
          <h2 class="panel-title">我的资料</h2>
          <el-form
            v-if="profileForm"
            :model="profileForm"
            label-width="80px"
            class="profile-form"
          >
            <el-form-item label="昵称">
              <el-input v-model="profileForm.name" placeholder="给自己起个昵称" maxlength="20" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.sex">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
                <el-radio value="保密">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker
                v-model="profileForm.birthday"
                type="date"
                placeholder="选择生日"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="example@mail.com" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="btn-primary"
                :loading="savingProfile"
                @click="saveProfile"
              >
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 收货地址 -->
        <div v-else-if="activePanel === 'address'" class="panel card">
          <div class="panel-head">
            <h2 class="panel-title">收货地址</h2>
            <el-button type="primary" class="btn-primary" round @click="openAddrDialog()">
              <el-icon><Plus /></el-icon>&nbsp;新增地址
            </el-button>
          </div>

          <div v-if="addresses.length" class="addr-list">
            <div v-for="a in addresses" :key="a.id" class="addr-item">
              <div class="addr-main">
                <div class="addr-top">
                  <span class="addr-name">{{ a.receiver }}</span>
                  <span class="addr-phone">{{ a.phone }}</span>
                  <el-tag v-if="a.isDefault" size="small" class="default-tag" round>
                    默认地址
                  </el-tag>
                </div>
                <p class="addr-detail">{{ a.addrDetail }}</p>
              </div>
              <div class="addr-ops">
                <el-button v-if="!a.isDefault" link type="primary" @click="onSetDefault(a)">
                  设为默认
                </el-button>
                <el-button link @click="openAddrDialog(a)">编辑</el-button>
                <el-button link type="danger" @click="onDeleteAddr(a)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有收货地址" />
        </div>

        <!-- 修改密码 -->
        <div v-else class="panel card">
          <h2 class="panel-title">修改密码</h2>
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            class="pwd-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                show-password
                placeholder="6-32 位新密码"
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="pwdForm.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="btn-primary"
                :loading="savingPwd"
                @click="savePassword"
              >
                确认修改
              </el-button>
            </el-form-item>
            <p class="pwd-tip">修改密码后需要重新登录哦～</p>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 地址编辑对话框 -->
    <el-dialog
      v-model="addrDialogVisible"
      :title="editingAddr ? '编辑地址' : '新增地址'"
      width="460px"
      align-center
    >
      <el-form ref="addrFormRef" :model="addrForm" :rules="addrRules" label-width="80px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addrForm.receiver" placeholder="收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addrForm.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" prop="addrDetail">
          <el-input
            v-model="addrForm.addrDetail"
            type="textarea"
            :rows="2"
            placeholder="省市区 + 街道门牌（如：浙江省杭州市西湖区文三路 100 号）"
            maxlength="120"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addrForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-primary" :loading="savingAddr" @click="saveAddr">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addAddress,
  deleteAddress,
  getProfile,
  setDefaultAddress,
  updateAddress,
  updateProfile,
} from '@/api/member'
import { changePassword } from '@/api/auth'
import type { MemberAddressVO, MemberProfileUpdateRequest, MemberVO } from '@/api/types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activePanel = ref('profile')
const profile = ref<MemberVO | null>(null)
const addresses = ref<MemberAddressVO[]>([])

const avatarText = computed(() => {
  const name = profile.value?.name || userStore.displayName || '会'
  return name.slice(0, 1)
})

/* ---------- 我的资料 ---------- */
const profileForm = ref<MemberProfileUpdateRequest | null>(null)
const savingProfile = ref(false)

async function saveProfile() {
  if (!profileForm.value) return
  savingProfile.value = true
  try {
    await updateProfile({
      name: profileForm.value.name?.trim() || undefined,
      sex: profileForm.value.sex,
      birthday: profileForm.value.birthday,
      phone: profileForm.value.phone,
      email: profileForm.value.email,
    })
    ElMessage.success('资料已更新')
    loadAll()
  } finally {
    savingProfile.value = false
  }
}

/* ---------- 地址管理 ---------- */
const addrDialogVisible = ref(false)
const savingAddr = ref(false)
const editingAddr = ref<MemberAddressVO | null>(null)
const addrFormRef = ref<FormInstance>()
const addrForm = reactive({
  receiver: '',
  phone: '',
  addrDetail: '',
  isDefault: false,
})

const addrRules: FormRules = {
  receiver: [{ required: true, message: '请填写收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  addrDetail: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
}

function openAddrDialog(addr?: MemberAddressVO) {
  editingAddr.value = addr ?? null
  addrForm.receiver = addr?.receiver ?? ''
  addrForm.phone = addr?.phone ?? ''
  addrForm.addrDetail = addr?.addrDetail ?? ''
  addrForm.isDefault = addr?.isDefault ?? addresses.value.length === 0
  addrDialogVisible.value = true
}

async function saveAddr() {
  await addrFormRef.value?.validate()
  savingAddr.value = true
  try {
    if (editingAddr.value) {
      await updateAddress(editingAddr.value.id, {
        receiver: addrForm.receiver.trim(),
        phone: addrForm.phone.trim(),
        addrDetail: addrForm.addrDetail.trim(),
        isDefault: addrForm.isDefault,
      })
      ElMessage.success('地址已更新')
    } else {
      await addAddress({
        receiver: addrForm.receiver.trim(),
        phone: addrForm.phone.trim(),
        addrDetail: addrForm.addrDetail.trim(),
        isDefault: addrForm.isDefault,
      })
      ElMessage.success('新增地址成功')
    }
    addrDialogVisible.value = false
    loadAll()
  } finally {
    savingAddr.value = false
  }
}

async function onSetDefault(addr: MemberAddressVO) {
  await setDefaultAddress(addr.id)
  ElMessage.success(`已将「${addr.receiver}」设为默认地址`)
  loadAll()
}

async function onDeleteAddr(addr: MemberAddressVO) {
  await ElMessageBox.confirm(
    `确定删除「${addr.receiver}」的这条地址吗？`,
    '删除地址',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
  )
  await deleteAddress(addr.id)
  ElMessage.success('地址已删除')
  loadAll()
}

/* ---------- 修改密码 ---------- */
const pwdFormRef = ref<FormInstance>()
const savingPwd = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function savePassword() {
  await pwdFormRef.value?.validate()
  savingPwd.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 800)
  } finally {
    savingPwd.value = false
  }
}

/* ---------- 加载 ---------- */
function onSelect(index: string) {
  activePanel.value = index
}

async function loadAll() {
  const res = await getProfile()
  profile.value = res.profile
  addresses.value = res.addresses
  profileForm.value = {
    name: res.profile.name ?? '',
    sex: res.profile.sex ?? '保密',
    birthday: res.profile.birthday ?? '',
    phone: res.profile.phone ?? '',
    email: res.profile.email ?? '',
  }
}

onMounted(loadAll)
</script>

<style scoped>
.user-page {
  padding-top: 24px;
  padding-bottom: 48px;
}

.user-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* ---------- 左侧卡 ---------- */
.profile-card {
  width: 240px;
  flex-shrink: 0;
  padding: 30px 0 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: sticky;
  top: 80px;
}

.avatar {
  background: var(--primary-gradient);
  color: #fff;
  font-size: 30px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(249, 115, 22, 0.35);
}

.profile-name {
  margin-top: 14px;
  font-size: 17px;
  font-weight: 700;
}

.profile-account {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-sub);
}

.profile-join {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-light);
}

.user-menu {
  width: 100%;
  margin-top: 20px;
  border-right: none;
}

:deep(.el-menu-item) {
  height: 46px;
  justify-content: center;
}

:deep(.el-menu-item.is-active) {
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 8px;
  margin: 0 10px;
}

/* ---------- 右侧面板 ---------- */
.panel-area {
  flex: 1;
  min-width: 0;
}

.panel {
  padding: 26px 30px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.panel-head .panel-title {
  margin-bottom: 0;
}

.profile-form,
.pwd-form {
  max-width: 440px;
}

.pwd-tip {
  font-size: 12px;
  color: var(--text-light);
  margin-left: 90px;
}

/* ---------- 地址列表 ---------- */
.addr-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.addr-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1.5px solid #eceae6;
  border-radius: var(--radius);
  transition: all 0.2s;
}

.addr-item:hover {
  border-color: #fdba8c;
}

.addr-main {
  flex: 1;
  min-width: 0;
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.addr-name {
  font-weight: 600;
}

.addr-phone {
  font-size: 13px;
  color: var(--text-sub);
}

.default-tag {
  background: var(--primary-light);
  color: var(--primary);
  border: none;
}

.addr-detail {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.5;
}

.addr-ops {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .user-layout {
    flex-direction: column;
  }

  .profile-card {
    width: 100%;
    position: static;
    padding-bottom: 0;
  }

  .user-menu {
    margin-top: 16px;
    border-top: 1px solid #f0ede9;
    display: flex;
  }

  :deep(.el-menu-item) {
    flex: 1;
    padding: 0 8px;
  }
}
</style>
