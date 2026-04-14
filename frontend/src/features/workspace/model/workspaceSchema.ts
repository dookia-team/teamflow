import { z } from 'zod'

export const createWorkspaceSchema = z.object({
  name: z
    .string()
    .min(2, '워크스페이스 이름은 2자 이상이어야 합니다')
    .max(100, '워크스페이스 이름은 100자 이하여야 합니다'),
})

export type CreateWorkspaceFormData = z.infer<typeof createWorkspaceSchema>
