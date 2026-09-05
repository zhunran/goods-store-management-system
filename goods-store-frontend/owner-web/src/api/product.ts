import { get } from '@/utils/request'
import type { BrandVO, CategoryTreeVO, GoodQuery, GoodVO } from './types'

export function brandList() {
  return get<BrandVO[]>('/brand/list')
}

export function goodList(query: GoodQuery) {
  return get<GoodVO[]>('/product/list', query)
}

export function categoryTree() {
  return get<CategoryTreeVO[]>('/product/tree')
}

export function goodDetail(id: string | number) {
  return get<GoodVO>(`/product/${id}`)
}
