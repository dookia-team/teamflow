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

export interface WorkspaceDetail {
  no: number
  name: string
  slug: string
  members: WorkspaceMember[]
}

export interface WorkspaceMember {
  no: number
  userNo: number
  role: string
  joinDate: string
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
