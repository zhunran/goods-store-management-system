/**
 * C 端接口类型定义（与后端 spi VO 对齐）
 * 注意：后端 Jackson 全局将 Long 序列化为 String（防 JS 精度丢失），
 * 因此所有 Long 字段（id/total/userId 等）在前端为 string；Integer/BigDecimal 为 number。
 */

// ---------- 认证 ----------
export interface UserInfo {
  userId: string;
  account: string;
  type: string;
  roles: string[];
  permissions: string[];
}

export interface LoginRequest {
  account: string;
  password: string;
  loginType?: string;
  captchaId?: string;
  captchaCode?: string;
}

export interface CaptchaVO {
  captchaId: string;
  image: string;
}

export interface RegisterRequest {
  account: string;
  password: string;
  phone?: string;
  email?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userInfo: UserInfo;
}

// ---------- 品牌 ----------
export interface BrandVO {
  id: string;
  name: string;
  company: string;
  logo: string;
  site: string;
  description: string;
  createdTime?: string;
  updatedTime?: string;
}

// ---------- 商品 ----------
export interface GoodVO {
  id: string;
  spuNo?: string;
  name: string;
  alias?: string;
  summary?: string;
  categoryId: number;
  brandId: number;
  isHot?: boolean;
  isDel?: boolean;
  isSeckill?: boolean;
  qty: number;
  description?: string;
  createdTime?: string;
  updatedTime?: string;
  markPrice?: number;
  price: number;
  pic: string;
  brandName?: string;
  categoryName?: string;
  detailPicList?: string[];
}

export interface CategoryTreeVO {
  id: string;
  parentId: number;
  name: string;
  icon?: string;
  children: CategoryTreeVO[];
}

export interface GoodQuery {
  pageNum?: number;
  pageSize?: number;
  name?: string;
  keyword?: string;
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  isHot?: boolean;
}

// ---------- 购物车 ----------
export interface CartItemVO {
  cartId: string;
  goodId: string;
  qty: number;
  selected: boolean;
  goodName: string;
  goodPic: string;
  price: number;
}

// ---------- 订单 ----------
/** 订单状态码：10 待付款 / 20 已支付 / 30 已发货 / 40 已完成 / 50 已取消 / 60 已退款 */
export type OrderStatus = "10" | "20" | "30" | "40" | "50" | "60";

export const ORDER_STATUS_TEXT: Record<string, string> = {
  "10": "待付款",
  "20": "已支付",
  "30": "已发货",
  "40": "已完成",
  "50": "已取消",
  "60": "已退款",
};

export const ORDER_STATUS_TAG: Record<string, string> = {
  "10": "warning",
  "20": "primary",
  "30": "info",
  "40": "success",
  "50": "danger",
  "60": "danger",
};

export interface OrderItemVO {
  goodId: string;
  dealPrice: number;
  count: number;
  goodName: string;
  goodPic: string;
}

export interface OrderVO {
  id: string;
  orderNo: string;
  memberAccount?: string;
  totalPay: number;
  payType?: string;
  status: OrderStatus | string;
  checkoutTime?: string;
  payTime?: string;
  shipTime?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface OrderDetailVO extends OrderVO {
  acceptTime?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddrDetail?: string;
  orderComment?: string;
  items: OrderItemVO[];
}

export interface OrderPageVO {
  total: string;
  pageNum: number;
  pageSize: number;
  records: OrderVO[];
}

export interface OrderCreateResponse {
  id: string;
  orderNo: string;
  totalPay: number;
  status: string;
}

// ---------- 会员 ----------
export interface MemberVO {
  id: string;
  account: string;
  name?: string;
  pinyin?: string;
  sex?: string;
  birthday?: string;
  phone?: string;
  maskedCardId?: string;
  email?: string;
  portrait?: string;
  lastLoginTime?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface MemberProfileUpdateRequest {
  name?: string;
  sex?: string;
  birthday?: string;
  portrait?: string;
  email?: string;
  phone?: string;
}

export interface MemberAddressVO {
  id: string;
  receiver: string;
  phone: string;
  addrId?: number;
  addrDetail: string;
  isDefault: boolean;
}

export interface MemberAddressRequest {
  receiver: string;
  phone: string;
  addrId?: number;
  addrDetail: string;
  isDefault?: boolean;
}

// ---------- 秒杀 ----------
export type SeckillStatus = "NOT_STARTED" | "IN_PROGRESS" | "ENDED";

export interface SeckillGoodVO {
  id: string;
  goodId: string;
  goodName: string;
  goodPic: string;
  originalPrice: number;
  description?: string;
  seckillId?: string;
  activityName?: string;
  startTime?: string;
  endTime?: string;
  status: SeckillStatus | string;
  /** 后端 Long 经全局 Jackson 序列化为字符串，如 "0" */
  countdownSec: string;
  /** 剩余库存（Redis 预热后返回；未预热为 null 不算售空。Long 经全局序列化为字符串） */
  stockLeft?: string | null;
  /** 当前用户是否已抢到 */
  robbed?: boolean;
}

/** 秒杀商品价格未在后端单独给出时前端展示用 */
export interface SeckillOrderResponse {
  orderNo: string;
  status: string;
  message?: string;
}

export interface SeckillOrderResultVO {
  orderNo: string;
  status: string;
  message?: string;
}
