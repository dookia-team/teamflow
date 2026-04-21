import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'

interface CreateTicketParams {
  projectNo: number
  data: {
    title: string
    description?: string
    status?: string
    priority?: string
    assigneeNo?: number
    dueDate?: string
  }
}

export function useCreateTicket() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ projectNo, data }: CreateTicketParams) => ticketApi.create(projectNo, data),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', variables.projectNo] })
    },
  })
}
