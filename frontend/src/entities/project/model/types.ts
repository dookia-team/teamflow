export interface Project {
  no: number
  workspaceNo: number
  name: string
  key: string
  description: string | null
  icon: string | null
  color: string | null
  visibility: 'PUBLIC' | 'PRIVATE'
  status: 'ACTIVE' | 'COMPLETED'
}

export interface ProjectSummary {
  no: number
  workspaceNo: number
  name: string
  key: string
  description: string | null
  memberCount: number
}
