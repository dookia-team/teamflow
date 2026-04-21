import { useQuery } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'

export function useTickets(projectNo: number | undefined) {
  return useQuery({
    queryKey: ['tickets', projectNo],
    queryFn: () => ticketApi.list(projectNo!),
    enabled: !!projectNo,
    select: (res) => res.data,
  })
}
