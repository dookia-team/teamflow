import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'

interface UpdateTicketParams {
  ticketNo: number
  projectNo: number
  data: {
    title?: string
    description?: string
    status?: string
    priority?: string
    assigneeNo?: number | null
    dueDate?: string | null
  }
}

export function useUpdateTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketNo, data }: UpdateTicketParams) => ticketApi.update(ticketNo, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', variables.projectNo] })
    },
  })
}
