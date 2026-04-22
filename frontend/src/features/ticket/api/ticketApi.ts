import { apiClient } from '@/shared/api/apiClient'
import type { Ticket, TicketStatus } from '@/entities/ticket'

interface CreateTicketRequest {
  title: string
  description?: string
  status?: TicketStatus
  priority?: string
  assigneeNo?: number
  dueDate?: string
}

interface UpdateTicketRequest {
  title?: string
  description?: string
  status?: TicketStatus
  priority?: string
  assigneeNo?: number | null
  dueDate?: string | null
}

export const ticketApi = {
  create: (projectNo: number, data: CreateTicketRequest) =>
    apiClient.post<Ticket>(`/api/projects/${projectNo}/tickets`, data),

  list: (projectNo: number) => apiClient.get<Ticket[]>(`/api/projects/${projectNo}/tickets`),

  getDetail: (ticketNo: number) => apiClient.get<Ticket>(`/api/tickets/${ticketNo}`),

  update: (ticketNo: number, data: UpdateTicketRequest) =>
    apiClient.patch<Ticket>(`/api/tickets/${ticketNo}`, data),

  delete: (ticketNo: number) => apiClient.delete(`/api/tickets/${ticketNo}`),

  updateStatus: (ticketNo: number, status: TicketStatus) =>
    apiClient.patch<Ticket>(`/api/tickets/${ticketNo}/status`, { status }),

  updatePosition: (ticketNo: number, position: number) =>
    apiClient.patch<Ticket>(`/api/tickets/${ticketNo}/position`, { position }),
}
