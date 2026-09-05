import { del, get, post, put } from '@/utils/request'
import type {
  MemberAddressRequest,
  MemberAddressVO,
  MemberProfileUpdateRequest,
  MemberVO,
} from './types'

export interface MemberProfileBundle {
  profile: MemberVO
  addresses: MemberAddressVO[]
}

export function getProfile() {
  return get<MemberProfileBundle>('/member/profile')
}

export function updateProfile(data: MemberProfileUpdateRequest) {
  return put<MemberVO>('/member/profile', data)
}

export function addAddress(data: MemberAddressRequest) {
  return post<MemberAddressVO>('/member/address', data)
}

export function updateAddress(addrId: string | number, data: MemberAddressRequest) {
  return put<MemberAddressVO>(`/member/address/${addrId}`, data)
}

export function deleteAddress(addrId: string | number) {
  return del<void>(`/member/address/${addrId}`)
}

export function setDefaultAddress(addrId: string | number) {
  return put<void>(`/member/address/${addrId}/default`)
}
