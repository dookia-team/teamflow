import { useQuery } from '@tanstack/react-query'
import { projectApi } from '../api/projectApi'

export function useProjects(workspaceNo: number | undefined) {
  return useQuery({
    queryKey: ['projects', workspaceNo],
    queryFn: () => projectApi.list(workspaceNo!),
    enabled: !!workspaceNo,
    select: (res) => res.data,
  })
}
