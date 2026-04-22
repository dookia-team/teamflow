import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ticketApi } from '../api/ticketApi'

interface UpdateTicketPositionParams {
  ticketNo: number
  projectNo: number
  position: number
}

export function useUpdateTicketPosition() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ ticketNo, position }: UpdateTicketPositionParams) =>
      ticketApi.updatePosition(ticketNo, position),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tickets', variables.projectNo] })
    },
  })
}
