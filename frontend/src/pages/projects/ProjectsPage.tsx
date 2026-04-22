import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { WorkspaceModal } from '@/features/workspace'
import { useWorkspaces } from '@/features/workspace'
import { useProjects, CreateProjectModal } from '@/features/project'
import { Button, Card, Spinner, Badge } from '@/shared/ui'

export function ProjectsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const isNewUser = (location.state as { isNewUser?: boolean })?.isNewUser ?? false
  const [showWorkspaceModal, setShowWorkspaceModal] = useState(isNewUser)
  const [showCreateProjectModal, setShowCreateProjectModal] = useState(false)

  if (isNewUser) {
    window.history.replaceState({}, '')
  }

  const handleWorkspaceCreated = () => {
    setShowWorkspaceModal(false)
  }

  const { data: workspaces, isLoading: isLoadingWorkspaces } = useWorkspaces()
  const currentWorkspace = workspaces?.[0]
  const workspaceNo = currentWorkspace?.no

  const { data: projects, isLoading: isLoadingProjects } = useProjects(workspaceNo)

  if (isLoadingWorkspaces) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!currentWorkspace) {
    return (
      <div className="bg-gray-50 min-h-full">
        <div className="max-w-5xl mx-auto px-fluid-page-x py-10 text-center">
          <h1 className="text-fluid-h1 font-bold text-gray-900 mb-4">워크스페이스가 없습니다</h1>
          <p className="text-gray-500 mb-6">먼저 워크스페이스를 만들어주세요.</p>
          <Button variant="primary" size="lg" onClick={() => setShowWorkspaceModal(true)}>
            워크스페이스 만들기
          </Button>
        </div>
        <WorkspaceModal
          isOpen={showWorkspaceModal}
          onClose={() => setShowWorkspaceModal(false)}
          onSuccess={handleWorkspaceCreated}
        />
      </div>
    )
  }

  const handleProjectClick = (projectNo: number) => {
    navigate(`/workspace/${workspaceNo}/project/${projectNo}`)
  }

  return (
    <div className="bg-gray-50 min-h-[calc(100vh-64px)]">
      <div className="max-w-5xl mx-auto px-fluid-page-x py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-fluid-h1 font-bold text-gray-900">내 프로젝트</h1>
            <p className="text-sm text-gray-500 mt-1">{currentWorkspace.name}</p>
          </div>
          <Button variant="primary" size="md" onClick={() => setShowCreateProjectModal(true)}>
            + 새 프로젝트
          </Button>
        </div>

        {isLoadingProjects ? (
          <div className="flex items-center justify-center py-20">
            <Spinner size="md" />
          </div>
        ) : projects && projects.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {projects.map((project) => (
              <Card
                key={project.no}
                className="cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => handleProjectClick(project.no)}
              >
                <div className="flex items-start justify-between mb-3">
                  <h3 className="text-base font-semibold text-gray-900 truncate">{project.name}</h3>
                  <Badge>{project.slug}</Badge>
                </div>
                {project.memberCount !== undefined && (
                  <p className="text-sm text-gray-500">{project.memberCount}명의 멤버</p>
                )}
              </Card>
            ))}

            <button
              className="border-2 border-dashed border-gray-300 rounded-card p-8 flex flex-col items-center justify-center gap-3 text-gray-400 hover:border-primary-400 hover:text-primary-500 transition-colors"
              onClick={() => setShowCreateProjectModal(true)}
            >
              <span className="text-4xl">+</span>
              <span className="font-semibold">새 프로젝트 만들기</span>
            </button>
          </div>
        ) : (
          <div className="text-center py-20">
            <p className="text-gray-400 text-lg mb-4">아직 프로젝트가 없습니다</p>
            <Button variant="primary" size="lg" onClick={() => setShowCreateProjectModal(true)}>
              첫 프로젝트 만들기
            </Button>
          </div>
        )}
      </div>

      <WorkspaceModal
        isOpen={showWorkspaceModal}
        onClose={() => setShowWorkspaceModal(false)}
        onSuccess={handleWorkspaceCreated}
      />

      {workspaceNo && (
        <CreateProjectModal
          isOpen={showCreateProjectModal}
          onClose={() => setShowCreateProjectModal(false)}
          workspaceNo={workspaceNo}
        />
      )}
    </div>
  )
}
