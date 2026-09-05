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
