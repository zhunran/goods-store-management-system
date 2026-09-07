export interface MemberVO {
  id: number
  account: string
  name: string
  pinyin: string
  sex: string
  birthday: string
  enabled: boolean
  phone: string
  maskedCardId: string
  email: string
  portrait: string
  lastLoginTime: string
  createdTime: string
  updatedTime: string
}

export interface MemberPageVO {
  total: number
  pageNum: number
  pageSize: number
  records: MemberVO[]
}
