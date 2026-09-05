import { defineStore } from "pinia";
import { login as loginApi, logout as logoutApi } from "@/api/auth";
import type { LoginRequest, UserInfo } from "@/api/types";

interface UserState {
  token: string;
  userInfo: UserInfo | null;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: localStorage.getItem("user_access_token") || "",
    userInfo: JSON.parse(localStorage.getItem("user_user_info") || "null"),
  }),
  getters: {
    isLogin: (state) => !!state.token,
    displayName: (state) =>
      state.userInfo?.account || "游客",
  },
  actions: {
    async login(data: LoginRequest) {
      const res = await loginApi(data);
      // 防御：后端异常被吞时 accessToken 会是 undefined，
      // 若存入 localStorage 会变成字符串 "undefined"，导致网关 401 而非登录页报错
      if (!res?.accessToken) {
        throw new Error("登录响应异常：未获取到有效 token");
      }
      this.token = res.accessToken;
      this.userInfo = res.userInfo;
      localStorage.setItem("user_access_token", res.accessToken);
      localStorage.setItem("user_refresh_token", res.refreshToken);
      localStorage.setItem("user_user_info", JSON.stringify(res.userInfo));
    },
    async logout() {
      try {
        await logoutApi();
      } catch {
        // 后端登出失败不阻塞本地清理
      }
      this.clearLocal();
    },
    clearLocal() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem("user_access_token");
      localStorage.removeItem("user_refresh_token");
      localStorage.removeItem("user_user_info");
    },
  },
});
