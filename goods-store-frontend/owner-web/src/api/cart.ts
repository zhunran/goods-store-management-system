import { del, get, post, put } from '@/utils/request'
import type { CartItemVO } from './types'

export function cartList() {
  return get<CartItemVO[]>('/cart/list')
}

export function cartAdd(goodId: string | number, qty = 1) {
  return post<void>('/cart/add', { goodId, qty })
}

export function cartUpdateQty(cartId: string | number, qty: number) {
  return put<void>(`/cart/${cartId}`, null, { qty })
}

export function cartRemove(cartId: string | number) {
  return del<void>(`/cart/${cartId}`)
}
