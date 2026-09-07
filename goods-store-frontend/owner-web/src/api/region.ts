import { get } from '@/utils/request'

/** 行政区划条目（Long 经后端全局序列化为 string） */
export interface RegionVO {
  id: string
  parentId?: string | null
  code: string
  name: string
  shortName?: string
  level?: number
}

/** 查询下级行政区划，parentId=0 取省级 */
export function listRegionChildren(parentId: number | string = 0) {
  return get<RegionVO[]>('/region/children', { parentId })
}