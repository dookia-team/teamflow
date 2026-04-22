import { Outlet, useParams } from 'react-router-dom'
import { useWorkspace } from '@/features/workspace'
import { useProjects } from '@/features/project'
import { WorkspaceSidebar } from '@/widgets/workspace-sidebar'
import { Spinner } from '@/shared/ui'

export function WorkspaceLayout() {
  const { wsNo, projectNo } = useParams<{ wsNo: string; projectNo: string }>()
  const workspaceNo = Number(wsNo)
  const projNo = Number(projectNo)

  const { data: workspace, isLoading: isLoadingWorkspace } = useWorkspace(workspaceNo)
  const { data: projects, isLoading: isLoadingProjects } = useProjects(workspaceNo)

  const currentProject = projects?.find((p) => p.no === projNo)

  if (isLoadingWorkspace || isLoadingProjects) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!workspace || !currentProject) {
    return (
      <div className="flex items-center justify-center h-screen text-gray-500">
        <p>워크스페이스 또는 프로젝트를 찾을 수 없습니다.</p>
      </div>
    )
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <WorkspaceSidebar
        projectName={currentProject.name}
        workspaceNo={workspaceNo}
        projectNo={projNo}
        members={workspace.members}
      />
      <main className="flex-1 overflow-auto bg-gray-50">
        <Outlet />
      </main>
    </div>
  )
}
