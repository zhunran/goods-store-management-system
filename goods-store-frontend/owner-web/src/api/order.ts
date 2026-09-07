import { get, post, put } from '@/utils/request'
import type { OrderCreateResponse, OrderDetailVO, OrderPageVO } from './types'

export function submitOrder(addressId?: string | number, comment?: string) {
  return post<OrderCreateResponse>('/order/submit', { addressId, comment })
}

export function orderPage(pageNum: number, pageSize: number, status?: string) {
  return get<OrderPageVO>('/order/page', { pageNum, pageSize, status })
}

export function orderDetail(id: string | number) {
  return get<OrderDetailVO>(`/order/${id}`)
}

export function cancelOrder(id: string | number) {
  return put<void>(`/order/${id}/cancel`)
}

export function confirmOrder(id: string | number) {
  return put<void>(`/order/${id}/confirm`)
}

/** 模拟支付：payType 为 ALIPAY | WECHAT */
export function payOrder(id: string | number, payType: string) {
  return put<void>(`/order/${id}/pay`, { payType })
}

/** 申请退款（仅已支付且未发货的订单，模拟退款即时到账） */
export function refundOrder(id: string | number) {
  return put<void>(`/order/${id}/refund`)
}
