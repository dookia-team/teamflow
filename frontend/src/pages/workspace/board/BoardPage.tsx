import { useState, useCallback, useMemo, useRef } from 'react'
import { useParams } from 'react-router-dom'
import {
  DndContext,
  DragOverlay,
  closestCorners,
  PointerSensor,
  useSensor,
  useSensors,
  useDroppable,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core'
import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import {
  useTickets,
  useUpdateTicketStatus,
  useUpdateTicketPosition,
  CreateTicketModal,
  TicketDetailPanel,
} from '@/features/ticket'
import { Button, Badge, Spinner } from '@/shared/ui'
import type { Ticket, TicketStatus } from '@/entities/ticket'

const columns: { status: TicketStatus; label: string }[] = [
  { status: 'BACKLOG', label: 'Backlog' },
  { status: 'TODO', label: 'To Do' },
  { status: 'IN_PROGRESS', label: 'In Progress' },
  { status: 'DONE', label: 'Done' },
]

const validStatuses = new Set<string>(columns.map((c) => c.status))

const priorityColors: Record<string, 'danger' | 'warning' | 'primary' | 'success'> = {
  CRITICAL: 'danger',
  HIGH: 'warning',
  MEDIUM: 'primary',
  LOW: 'success',
}

export function BoardPage() {
  const { projectNo } = useParams<{ projectNo: string }>()
  const projNo = Number(projectNo)
  const { data: tickets, isLoading } = useTickets(projNo)
  const updateStatus = useUpdateTicketStatus()
  const updatePosition = useUpdateTicketPosition()
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null)
  const [activeTicket, setActiveTicket] = useState<Ticket | null>(null)
  const isDraggingRef = useRef(false)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
  )

  const getTicketsByStatus = useCallback(
    (status: TicketStatus): Ticket[] => {
      if (!tickets) return []
      return tickets
        .filter((ticket) => ticket.status === status)
        .sort((a, b) => a.position - b.position)
    },
    [tickets],
  )

  const ticketIdsByColumn = useMemo(() => {
    const result: Record<TicketStatus, string[]> = {
      BACKLOG: [],
      TODO: [],
      IN_PROGRESS: [],
      DONE: [],
    }
    if (!tickets) return result
    for (const col of columns) {
      result[col.status] = getTicketsByStatus(col.status).map((t) => `ticket-${t.no}`)
    }
    return result
  }, [tickets, getTicketsByStatus])

  const findTicketById = useCallback(
    (id: string): Ticket | undefined => {
      if (!tickets) return undefined
      const no = Number(id.replace('ticket-', ''))
      return tickets.find((t) => t.no === no)
    },
    [tickets],
  )

  const handleDragStart = useCallback(
    (event: DragStartEvent) => {
      isDraggingRef.current = true
      const ticket = findTicketById(String(event.active.id))
      if (ticket) setActiveTicket(ticket)
    },
    [findTicketById],
  )

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      setTimeout(() => {
        isDraggingRef.current = false
      }, 0)
      setActiveTicket(null)
      const { active, over } = event
      if (!over || !tickets) return

      const activeId = String(active.id)
      const overId = String(over.id)
      const activeTicketData = findTicketById(activeId)
      if (!activeTicketData) return

      // Determine target column
      let targetStatus: TicketStatus | undefined
      const overTicket = findTicketById(overId)
      if (overTicket) {
        targetStatus = overTicket.status
      } else if (validStatuses.has(overId)) {
        targetStatus = overId as TicketStatus
      }
      if (!targetStatus) return

      const sourceStatus = activeTicketData.status

      if (sourceStatus !== targetStatus) {
        updateStatus.mutate({
          ticketNo: activeTicketData.no,
          projectNo: projNo,
          status: targetStatus,
        })
      } else if (activeId !== overId && overTicket) {
        const columnTickets = getTicketsByStatus(sourceStatus)
        const overIndex = columnTickets.findIndex((t) => t.no === overTicket.no)
        if (overIndex >= 0) {
          updatePosition.mutate({
            ticketNo: activeTicketData.no,
            projectNo: projNo,
            position: overIndex,
          })
        }
      }
    },
    [tickets, findTicketById, getTicketsByStatus, updateStatus, updatePosition, projNo],
  )

  const handleTicketClick = useCallback((ticket: Ticket) => {
    if (isDraggingRef.current) return
    setSelectedTicket(ticket)
  }, [])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Spinner size="lg" />
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col">
      {/* Board Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-grey-200">
        <h1 className="text-lg font-bold text-grey-900">Board</h1>
        <Button variant="primary" size="sm" onClick={() => setShowCreateModal(true)}>
          + 티켓 만들기
        </Button>
      </div>

      {/* Kanban Columns with DnD */}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <div className="flex-1 flex gap-4 p-6 overflow-x-auto">
          {columns.map((col) => {
            const columnTickets = getTicketsByStatus(col.status)
            return (
              <KanbanColumn
                key={col.status}
                status={col.status}
                label={col.label}
                tickets={columnTickets}
                ticketIds={ticketIdsByColumn[col.status]}
                onTicketClick={handleTicketClick}
              />
            )
          })}
        </div>

        <DragOverlay>
          {activeTicket ? <TicketCardOverlay ticket={activeTicket} /> : null}
        </DragOverlay>
      </DndContext>

      {/* Create Ticket Modal */}
      {projNo && (
        <CreateTicketModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          projectNo={projNo}
        />
      )}

      {/* Ticket Detail Panel */}
      <TicketDetailPanel
        ticket={selectedTicket}
        open={selectedTicket !== null}
        onClose={() => setSelectedTicket(null)}
        projectNo={projNo}
      />
    </div>
  )
}

function KanbanColumn({
  status,
  label,
  tickets,
  ticketIds,
  onTicketClick,
}: {
  status: TicketStatus
  label: string
  tickets: Ticket[]
  ticketIds: string[]
  onTicketClick: (ticket: Ticket) => void
}) {
  const { setNodeRef } = useDroppable({
    id: status,
    data: { type: 'column' },
  })

  return (
    <div className="flex-1 min-w-[280px] flex flex-col">
      {/* Column Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-bold text-grey-700">{label}</h3>
          <span className="text-xs text-grey-500 bg-grey-100 px-1.5 py-0.5 rounded-full">
            {tickets.length}
          </span>
        </div>
      </div>

      {/* Column Body */}
      <SortableContext items={ticketIds} strategy={verticalListSortingStrategy}>
        <div ref={setNodeRef} className="flex-1 bg-grey-50 rounded-lg p-2 space-y-2 min-h-[200px]">
          {tickets.length === 0 ? (
            <p className="text-center text-sm text-grey-400 py-8">티켓 없음</p>
          ) : (
            tickets.map((ticket) => (
              <SortableTicketCard
                key={ticket.no}
                ticket={ticket}
                onClick={() => onTicketClick(ticket)}
              />
            ))
          )}
        </div>
      </SortableContext>
    </div>
  )
}

function SortableTicketCard({ ticket, onClick }: { ticket: Ticket; onClick: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: `ticket-${ticket.no}`,
    data: { type: 'ticket', ticket },
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners} onClick={onClick}>
      <TicketCardContent ticket={ticket} />
    </div>
  )
}

function TicketCardContent({ ticket }: { ticket: Ticket }) {
  return (
    <div className="bg-white rounded-lg border border-grey-200 p-3 hover:shadow-sm transition-shadow cursor-pointer">
      <div className="flex items-start justify-between mb-2">
        <span className="text-xs text-grey-500 font-medium">{ticket.ticketKey}</span>
        <Badge variant={priorityColors[ticket.priority] ?? 'default'} size="xs">
          {ticket.priority}
        </Badge>
      </div>
      <p className="text-sm font-medium text-grey-900 mb-2 line-clamp-2">{ticket.title}</p>
      <div className="flex items-center justify-between text-xs text-grey-500">
        {ticket.dueDate && <span>{ticket.dueDate}</span>}
      </div>
    </div>
  )
}

function TicketCardOverlay({ ticket }: { ticket: Ticket }) {
  return (
    <div className="rotate-3 shadow-lg">
      <TicketCardContent ticket={ticket} />
    </div>
  )
}
