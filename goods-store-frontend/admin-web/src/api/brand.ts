export interface BrandVO {
  id: number
  name: string
  company: string
  logo: string
  site: string
  description: string
  createdTime: string
  updatedTime: string
}

export interface PageVO<T> {
  total: number
  records: T[]
}
