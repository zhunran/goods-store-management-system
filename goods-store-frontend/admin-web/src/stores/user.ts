import { defineStore } from "pinia";
import { adminLogin } from "@/api/auth";
import type { LoginRequest, UserInfo } from "@/api/types";

interface UserState {
  token: string;
  userInfo: UserInfo | null;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: localStorage.getItem("admin_access_token") || "",
    userInfo: JSON.parse(localStorage.getItem("admin_user_info") || "null"),
  }),
  getters: {
    permissions: (state) => state.userInfo?.permissions ?? [],
    roles: (state) => state.userInfo?.roles ?? [],
  },
  actions: {
    async login(data: LoginRequest) {
      const res = await adminLogin(data);
      // 防御：后端异常被吞时 accessToken 会是 undefined，
      // 若存入 localStorage 会变成字符串 "undefined"，导致网关 401 而非登录页报错
      if (!res?.accessToken) {
        throw new Error("登录响应异常：未获取到有效 token");
      }
      this.token = res.accessToken;
      this.userInfo = res.userInfo;
      localStorage.setItem("admin_access_token", res.accessToken);
      localStorage.setItem("admin_refresh_token", res.refreshToken);
      localStorage.setItem("admin_user_info", JSON.stringify(res.userInfo));
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem("admin_access_token");
      localStorage.removeItem("admin_refresh_token");
      localStorage.removeItem("admin_user_info");
    },
    hasPermission(code: string) {
      return this.permissions.includes(code) || this.permissions.includes("*");
    },
  },
});
