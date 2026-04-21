export type TicketStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface Ticket {
  no: number
  ticketKey: string
  title: string
  description: string | null
  status: TicketStatus
  priority: TicketPriority
  assigneeNo: number | null
  position: number
  dueDate: string | null
  createDate: string
  updateDate: string
}
