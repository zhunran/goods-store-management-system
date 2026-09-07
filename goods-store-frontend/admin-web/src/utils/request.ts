import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

interface ApiResult<T = any> {
  code: number;
  success: boolean;
  message: string;
  data: T;
}

const service: AxiosInstance = axios.create({
  baseURL: "/app/api",
  timeout: 15000,
});

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem("admin_access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers["X-Trace-Id"] = crypto.randomUUID?.() ?? `${Date.now()}`;
  return config;
});

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data;
    // 直接返回业务数据（HTTP 2xx 视为成功；Void 接口 code 可能为 null/undefined）
    if (
      res.code === 200 ||
      res.success ||
      res.code === null ||
      res.code === undefined
    ) {
      return res.data as any;
    }
    // 401 未登录/过期
    if (res.code === 401) {
      handleUnauthorized();
      return Promise.reject(new Error(res.message || "未登录"));
    }
    ElMessage.error(res.message || "请求失败");
    return Promise.reject(new Error(res.message || "请求失败"));
  },
  async (error) => {
    const status = error.response?.status;
    const msg = error.response?.data?.message || error.message || "网络错误";
    const config = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (status === 401 && config && !config._retry) {
      config._retry = true;
      try {
        const token = await refreshAccessToken();
        config.headers.Authorization = `Bearer ${token}`;
        return service(config);
      } catch {
        handleUnauthorized();
        // 登录已过期：已提示并跳转登录页，挂起当前请求链，避免调用方产生未捕获的 Promise 异常
        return new Promise(() => {});
      }
    }

    if (status === 401) {
      handleUnauthorized();
    } else if (status === 403) {
      ElMessage.error("无权限访问");
    } else {
      ElMessage.error(msg);
    }
    return Promise.reject(error);
  },
);

let refreshPromise: Promise<string> | null = null;

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function doRefresh(): Promise<string> {
  const refreshToken = localStorage.getItem("admin_refresh_token");
  if (!refreshToken) {
    throw new Error("登录已过期");
  }
  const res = await axios.post<
    ApiResult<{ accessToken: string; refreshToken: string }>
  >("/app/api/auth/admin/refresh", { refreshToken });
  const data = res.data?.data;
  if (!data?.accessToken) {
    throw new Error("刷新失败");
  }
  localStorage.setItem("admin_access_token", data.accessToken);
  if (data.refreshToken) {
    localStorage.setItem("admin_refresh_token", data.refreshToken);
  }
  return data.accessToken;
}

function handleUnauthorized() {
  localStorage.removeItem("admin_access_token");
  localStorage.removeItem("admin_refresh_token");
  localStorage.removeItem("admin_user_info");
  if (router.currentRoute.value.path !== "/login") {
    ElMessage.warning("登录已过期，请重新登录");
    router.push("/login");
  }
}

export function get<T = any>(url: string, params?: object): Promise<T> {
  return service.get(url, { params }) as unknown as Promise<T>;
}

export function post<T = any>(
  url: string,
  data?: object,
  params?: object,
): Promise<T> {
  return service.post(url, data, { params }) as unknown as Promise<T>;
}

export function put<T = any>(
  url: string,
  data?: object | null,
  params?: object,
): Promise<T> {
  return service.put(url, data, { params }) as unknown as Promise<T>;
}

export function del<T = any>(url: string): Promise<T> {
  return service.delete(url) as unknown as Promise<T>;
}

export default service;
