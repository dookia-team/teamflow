import { useNavigate, useLocation } from 'react-router-dom'
import { Badge } from '@/shared/ui'
import type { WorkspaceMember } from '@/entities/workspace'

interface WorkspaceSidebarProps {
  projectName: string
  workspaceNo: number
  projectNo: number
  members: WorkspaceMember[]
}

const tabs = [
  { id: 'board', label: 'Board', icon: '▦', enabled: true },
  { id: 'docs', label: 'Docs', icon: '📄', enabled: false },
  { id: 'chat', label: 'Chat', icon: '💬', enabled: false },
] as const

const roleLabels: Record<string, string> = {
  OWNER: '오너',
  ADMIN: '관리자',
  MEMBER: '멤버',
}

export function WorkspaceSidebar({
  projectName,
  workspaceNo,
  projectNo,
  members,
}: WorkspaceSidebarProps) {
  const navigate = useNavigate()
  const location = useLocation()

  const activeTab = location.pathname.includes('/docs')
    ? 'docs'
    : location.pathname.includes('/chat')
      ? 'chat'
      : 'board'

  return (
    <aside className="w-60 border-r border-gray-200 bg-white flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-gray-100">
        <button
          className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-700 transition-colors mb-3"
          onClick={() => navigate('/projects')}
        >
          <span>←</span>
          <span>프로젝트 허브</span>
        </button>
        <h2 className="text-base font-bold text-gray-900 truncate">{projectName}</h2>
      </div>

      {/* Tabs */}
      <nav className="p-3">
        <ul className="space-y-1">
          {tabs.map((tab) => (
            <li key={tab.id}>
              <button
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
                  activeTab === tab.id
                    ? 'bg-primary-50 text-primary-700 font-semibold'
                    : tab.enabled
                      ? 'text-gray-600 hover:bg-gray-50'
                      : 'text-gray-400 cursor-not-allowed'
                }`}
                disabled={!tab.enabled}
                onClick={() => {
                  if (tab.enabled) {
                    navigate(`/workspace/${workspaceNo}/project/${projectNo}`)
                  }
                }}
              >
                <span>{tab.icon}</span>
                <span>{tab.label}</span>
                {!tab.enabled && <Badge className="ml-auto text-[10px]">Soon</Badge>}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      {/* Members */}
      <div className="border-t border-gray-100 p-3 mt-auto">
        <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-3 mb-2">
          멤버 ({members.length})
        </h3>
        <ul className="space-y-1">
          {members.map((member) => (
            <li key={member.no} className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm">
              <div className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-bold shrink-0">
                {member.userNo}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-gray-700 text-sm truncate">User {member.userNo}</p>
                <p className="text-gray-400 text-xs">{roleLabels[member.role] ?? member.role}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  )
}
