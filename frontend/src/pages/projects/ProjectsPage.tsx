import { useState, useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuthStore, LogoutButton } from '@/features/auth'
import { WorkspaceModal } from '@/features/workspace'
import { Avatar, Button } from '@/shared/ui'

export function ProjectsPage() {
  const user = useAuthStore((s) => s.user)
  const location = useLocation()
  const [showWorkspaceModal, setShowWorkspaceModal] = useState(false)

  useEffect(() => {
    if ((location.state as { isNewUser?: boolean })?.isNewUser) {
      setShowWorkspaceModal(true)
      window.history.replaceState({}, '')
    }
  }, [location.state])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Nav */}
      <header className="bg-white border-b border-gray-200 px-fluid-page-x py-4 flex items-center justify-between">
        <span className="text-xl font-bold text-gray-900">
          Team<span className="text-primary-600">Flow</span>
        </span>
        <div className="flex items-center gap-4">
          {user && (
            <Avatar src={user.picture} name={user.name} size="sm" />
          )}
          <LogoutButton />
        </div>
      </header>

      {/* Content */}
      <main className="max-w-5xl mx-auto px-8 py-10">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-gray-900">내 프로젝트</h1>
          <Button variant="primary" size="md">
            + 새 프로젝트
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* 새 프로젝트 만들기 카드 */}
          <button className="border-2 border-dashed border-gray-300 rounded-card p-8 flex flex-col items-center justify-center gap-3 text-gray-400 hover:border-primary-400 hover:text-primary-500 transition-colors">
            <span className="text-4xl">+</span>
            <span className="font-semibold">새 프로젝트 만들기</span>
          </button>
        </div>
      </main>

      {/* Workspace Modal */}
      <WorkspaceModal
        isOpen={showWorkspaceModal}
        onClose={() => setShowWorkspaceModal(false)}
      />
    </div>
  )
}
