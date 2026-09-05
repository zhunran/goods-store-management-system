export interface OrderVO {
  id: number
  orderNo: string
  memberAccount: string
  totalPay: number
  payType: string
  status: string
  checkoutTime: string
  payTime: string
  shipTime: string
  createdTime: string
  updatedTime: string
}

export interface OrderItemVO {
  goodId: number
  dealPrice: number
  count: number
  goodName: string
  goodPic: string
}

export interface OrderDetailVO {
  id: number
  orderNo: string
  memberAccount: string
  totalPay: number
  payType: string
  status: string
  checkoutTime: string
  payTime: string
  shipTime: string
  acceptTime: string
  receiverName: string
  receiverPhone: string
  receiverAddrDetail: string
  orderComment: string
  items: OrderItemVO[]
}

export interface OrderPageVO<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}
