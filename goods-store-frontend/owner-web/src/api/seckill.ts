import { get, post } from '@/utils/request'
import type { SeckillGoodVO, SeckillOrderResponse, SeckillOrderResultVO } from './types'

export function seckillList() {
  return get<SeckillGoodVO[]>('/seckill/list')
}

export function seckillOrder(seckillGoodId: string | number) {
  return post<SeckillOrderResponse>('/seckill/order', undefined, { seckillGoodId })
}

export function seckillResult(orderNo: string) {
  return get<SeckillOrderResultVO>(`/seckill/order/${orderNo}/result`)
}
