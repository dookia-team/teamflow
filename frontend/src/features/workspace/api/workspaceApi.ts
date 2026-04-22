import { apiClient } from '@/shared/api/apiClient'
import type {
  CreateWorkspaceResponse,
  WorkspaceSummary,
  WorkspaceDetail,
  InviteResponse,
} from '@/entities/workspace'

export const workspaceApi = {
  create: (data: { name: string }) =>
    apiClient.post<CreateWorkspaceResponse>('/api/workspaces', data),

  list: () => apiClient.get<WorkspaceSummary[]>('/api/workspaces'),

  getDetail: (workspaceNo: number) =>
    apiClient.get<WorkspaceDetail>(`/api/workspaces/${workspaceNo}`),

  invite: (workspaceNo: number, data: { inviteeUserNo: number; role: string }) =>
    apiClient.post<InviteResponse>(`/api/workspaces/${workspaceNo}/invite`, data),
}
