import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Modal, Button, Input, Select, Textarea, Spinner } from '@/shared/ui'
import { useCreateTicket } from '../model/useCreateTicket'

const createTicketSchema = z.object({
  title: z.string().min(2, '제목은 2자 이상이어야 합니다').max(200, '제목은 200자 이하여야 합니다'),
  description: z.string().optional(),
  status: z.string().default('BACKLOG'),
  priority: z.string().default('MEDIUM'),
  assigneeNo: z.string().optional(),
  dueDate: z.string().optional(),
})

type CreateTicketFormData = z.infer<typeof createTicketSchema>

interface CreateTicketModalProps {
  isOpen: boolean
  onClose: () => void
  projectNo: number
}

const statusOptions = [
  { label: 'Backlog', value: 'BACKLOG' },
  { label: 'To Do', value: 'TODO' },
  { label: 'In Progress', value: 'IN_PROGRESS' },
  { label: 'Done', value: 'DONE' },
]

const priorityOptions = [
  { label: 'Low', value: 'LOW' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'High', value: 'HIGH' },
  { label: 'Critical', value: 'CRITICAL' },
]

export function CreateTicketModal({ isOpen, onClose, projectNo }: CreateTicketModalProps) {
  const createTicket = useCreateTicket()

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateTicketFormData>({
    resolver: zodResolver(createTicketSchema),
    defaultValues: {
      title: '',
      description: '',
      status: 'BACKLOG',
      priority: 'MEDIUM',
    },
  })

  const isLoading = createTicket.isPending

  const onSubmit = async (data: CreateTicketFormData) => {
    try {
      await createTicket.mutateAsync({
        projectNo,
        data: {
          title: data.title,
          description: data.description || undefined,
          status: data.status,
          priority: data.priority,
          dueDate: data.dueDate || undefined,
        },
      })
      reset()
      onClose()
    } catch {
      // error handled by mutation state
    }
  }

  const handleClose = () => {
    reset()
    createTicket.reset()
    onClose()
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose} className="max-w-[560px] p-8">
      <h2 className="text-xl font-bold text-grey-900 mb-1">새 티켓 만들기</h2>
      <p className="text-sm text-grey-500 mb-6">티켓 정보를 입력하세요.</p>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="제목"
          placeholder="티켓 제목을 입력하세요"
          error={errors.title?.message}
          {...register('title')}
        />

        <Textarea
          label="설명 (선택)"
          placeholder="티켓에 대한 설명을 입력하세요"
          {...register('description')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Select label="상태" options={statusOptions} {...register('status')} />
          <Select label="우선순위" options={priorityOptions} {...register('priority')} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Input label="기한 (선택)" type="date" {...register('dueDate')} />
          <div />
        </div>

        {createTicket.isError && (
          <p className="text-sm text-error">티켓 생성에 실패했습니다. 다시 시도해주세요.</p>
        )}

        <div className="flex gap-3 pt-2">
          <Button type="button" variant="ghost" size="lg" className="flex-1" onClick={handleClose}>
            취소
          </Button>
          <Button type="submit" variant="primary" size="lg" className="flex-1" disabled={isLoading}>
            {isLoading ? (
              <Spinner size="sm" className="border-white border-t-transparent" />
            ) : (
              '티켓 생성'
            )}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
