import { get, put } from '@/utils/request'

export interface RoleVO {
  id: number
  code: string
  name: string
}

export interface PermissionVO {
  id: number
  code: string
  name: string
}

export function listRoles() {
  return get<RoleVO[]>('/role/list')
}

export function listPermissions() {
  return get<PermissionVO[]>('/role/permission/list')
}

export function listRolePermissions(roleId: number) {
  return get<number[]>(`/role/${roleId}/permissions`)
}

export function assignRolePermissions(roleId: number, permissionIds: number[]) {
  return put<void>(`/role/${roleId}/permissions`, { permissionIds })
}