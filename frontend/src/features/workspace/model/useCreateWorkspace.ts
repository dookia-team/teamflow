import { useMutation } from '@tanstack/react-query'
import { workspaceApi } from '../api/workspaceApi'

export function useCreateWorkspace() {
  const createMutation = useMutation({
    mutationFn: (data: { name: string }) => workspaceApi.create(data),
  })

  const inviteMutation = useMutation({
    mutationFn: ({ workspaceNo, inviteeUserNo, role }: { workspaceNo: number; inviteeUserNo: number; role: string }) =>
      workspaceApi.invite(workspaceNo, { inviteeUserNo, role }),
  })

  return { createMutation, inviteMutation }
}
