import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

/**
 * 后端统一返回结构。
 * 注意：Void 类接口成功时 ApiResult.success() 无参版本返回 code=null/success=false（HTTP 200），
 * 因此成功与否以 HTTP 2xx 为准，code 为 null 亦视为成功。
 */
interface ApiResult<T = any> {
  code: number | null;
  success: boolean;
  message: string;
  data: T;
}

const service: AxiosInstance = axios.create({
  baseURL: "/app/api",
  timeout: 15000,
});

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem("user_access_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.headers["X-Trace-Id"] = crypto.randomUUID?.() ?? `${Date.now()}`;
  return config;
});

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data;
    // HTTP 2xx 即成功；code=200 或 code=null（Void 接口）均返回业务数据
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
    const body = error.response?.data;
    const code = body?.code;
    let msg = body?.message || error.message || "网络开小差啦，请稍后再试";
    const config = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // 401：优先尝试刷新 token 后重放一次
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
    } else if (code === 4004) {
      // 秒杀专属文案
      msg = "手慢了，秒杀商品已被抢空";
    } else if (code === 4005) {
      msg = "您已参与过该秒杀，不能重复抢购哦";
    } else if (status === 403) {
      msg = "无权限访问";
    }
    ElMessage.error(msg);
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
  const refreshToken = localStorage.getItem("user_refresh_token");
  if (!refreshToken) {
    throw new Error("登录已过期");
  }
  const res = await axios.post<
    ApiResult<{ accessToken: string; refreshToken: string }>
  >("/app/api/auth/refresh", { refreshToken });
  const data = res.data?.data;
  if (!data?.accessToken) {
    throw new Error("刷新失败");
  }
  localStorage.setItem("user_access_token", data.accessToken);
  if (data.refreshToken) {
    localStorage.setItem("user_refresh_token", data.refreshToken);
  }
  return data.accessToken;
}

function handleUnauthorized() {
  localStorage.removeItem("user_access_token");
  localStorage.removeItem("user_refresh_token");
  localStorage.removeItem("user_user_info");
  if (router.currentRoute.value.path !== "/login") {
    ElMessage.warning("登录已过期，请重新登录");
    router.push({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath },
    });
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

export function del<T = any>(url: string, params?: object): Promise<T> {
  return service.delete(url, { params }) as unknown as Promise<T>;
}

export default service;
