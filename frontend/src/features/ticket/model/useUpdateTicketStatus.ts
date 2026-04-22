import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'
import type { TicketStatus } from '@/entities/ticket'

interface UpdateTicketStatusParams {
  ticketNo: number
  projectNo: number
  status: TicketStatus
}

export function useUpdateTicketStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketNo, status }: UpdateTicketStatusParams) =>
      ticketApi.updateStatus(ticketNo, status),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', variables.projectNo] })
    },
  })
}
