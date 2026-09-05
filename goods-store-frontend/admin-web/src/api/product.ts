export interface GoodVO {
  id: number
  spuNo: string
  name: string
  alias: string
  summary: string
  categoryId: number
  brandId: number
  isHot: boolean
  isDel: boolean
  isSeckill: boolean
  qty: number
  description: string
  createdTime: string
  updatedTime: string
  markPrice: number
  price: number
  pic: string
  brandName: string
  categoryName: string
  detailPicList: string[]
}

export interface GoodCreateRequest {
  name: string
  alias?: string
  summary?: string
  categoryId?: number
  brandId?: number
  markPrice?: number
  price?: number
  qty?: number
  pic?: string
  detail?: string
  isTakeDown?: boolean
  isHot?: boolean
  detailPicList?: string[]
}

export interface GoodUpdateRequest extends GoodCreateRequest {
  id: number
}

export interface CategoryTreeVO {
  id: number
  parentId: number
  name: string
  icon: string
  children: CategoryTreeVO[]
}
