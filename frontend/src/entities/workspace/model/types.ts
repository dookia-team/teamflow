export interface Workspace {
  no: number
  name: string
  slug: string
  role: string
  memberCount: number
}

export interface WorkspaceSummary {
  no: number
  name: string
  slug: string
  memberCount: number
}

export interface CreateWorkspaceResponse {
  no: number
  name: string
  slug: string
  role: string
  memberCount: number
}

export interface InviteResponse {
  no: number
  workspaceNo: number
  inviteeUserNo: number
  role: string
  status: string
  token: string
  expireDate: string
}
