export interface SeckillVO {
  id: number
  name: string
  enabled: boolean
  startTime: string
  endTime: string
  description: string
  createdTime: string
  updatedTime: string
}

export interface SeckillPageVO<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

export interface SeckillGoodVO {
  id: number
  goodId: number
  goodName: string
  goodPic: string
  originalPrice: number
  description: string
  seckillId: number
  activityName: string
  startTime: string
  endTime: string
  status: string
  countdownSec: number
}

export interface SeckillActivityRequest {
  name: string
  enabled: boolean
  startTime: string
  endTime: string
  description?: string
}

export interface SeckillGoodAddRequest {
  goodId: number
  description?: string
}
