import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'

interface DeleteTicketParams {
  ticketNo: number
  projectNo: number
}

export function useDeleteTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketNo }: DeleteTicketParams) => ticketApi.delete(ticketNo),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', variables.projectNo] })
    },
  })
}
