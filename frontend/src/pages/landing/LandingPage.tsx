import { Link } from 'react-router-dom'
import { Button } from '@/shared/ui'

export function LandingPage() {
  return (
    <div className="min-h-screen bg-[#0F172A] text-white">
      {/* Nav */}
      <nav className="flex items-center justify-between px-fluid-page-x py-6">
        <span className="text-2xl font-bold">
          Team<span className="text-primary-400">Flow</span>
        </span>
        <Link to="/login">
          <Button variant="ghost" size="sm" className="text-white/80 hover:text-white hover:bg-white/10">
            로그인
          </Button>
        </Link>
      </nav>

      {/* Hero - 2 Column Layout */}
      <main className="flex flex-col lg:flex-row items-center lg:justify-between px-fluid-page-x pt-fluid-section pb-20 gap-12 lg:gap-16">
        {/* Left: Text */}
        <div className="flex-1 max-w-xl text-center lg:text-left">
          <h1 className="text-fluid-display font-bold leading-tight mb-6">
            팀의 협업을
            <br />
            <span className="text-primary-400">하나로 연결합니다.</span>
          </h1>
          <p className="text-lg text-slate-400 mb-10 leading-relaxed">
            워크스페이스 기반 프로젝트 관리로 팀의 생산성을 극대화하세요.
            <br />
            실시간 협업, 일정 관리, 업무 추적을 하나의 플랫폼에서.
          </p>
          <Link to="/login">
            <Button variant="primary" size="lg">
              Google로 시작하기
            </Button>
          </Link>
        </div>

        {/* Right: App Preview Mockup */}
        <div className="flex-1 max-w-[620px] hidden md:block">
          <div className="bg-[#1E293B] rounded-2xl border border-[#334155] shadow-[0_20px_40px_rgba(99,102,241,0.2)] overflow-hidden">
            <div className="flex">
              {/* Sidebar */}
              <div className="w-[140px] bg-[#0F172A] p-4 space-y-3">
                <div className="text-primary-400 font-bold text-sm mb-4">TF</div>
                <div className="h-8 bg-primary-600 rounded-lg" />
                <div className="h-3 bg-[#334155] rounded w-20" />
                <div className="h-3 bg-[#334155] rounded w-24" />
                <div className="h-3 bg-[#334155] rounded w-16" />
              </div>

              {/* Main Content */}
              <div className="flex-1 p-4 space-y-4">
                {/* Top Bar */}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-200">내 프로젝트</span>
                  <div className="w-6 h-6 rounded-full bg-primary-500" />
                </div>

                {/* Stat Cards */}
                <div className="flex gap-3">
                  {[
                    { num: '12', label: '진행중', color: 'text-white' },
                    { num: '5', label: '완료', color: 'text-success' },
                    { num: '3', label: '대기중', color: 'text-warning' },
                  ].map((s) => (
                    <div key={s.label} className="flex-1 bg-[#334155] rounded-xl p-3">
                      <div className={`text-xl font-bold ${s.color}`}>{s.num}</div>
                      <div className="text-[10px] text-slate-400">{s.label}</div>
                    </div>
                  ))}
                </div>

                {/* Chart Area */}
                <div className="bg-[#334155] rounded-xl p-3">
                  <div className="text-xs font-semibold text-slate-200 mb-3">주간 활동</div>
                  <div className="flex items-end gap-2 h-24">
                    {[60, 90, 45, 110, 75, 130, 55].map((h, i) => (
                      <div
                        key={i}
                        className="flex-1 bg-primary-500 rounded"
                        style={{ height: `${(h / 130) * 100}%`, opacity: 0.6 + (i % 3) * 0.15 }}
                      />
                    ))}
                  </div>
                </div>

                {/* Task List */}
                <div className="bg-[#334155] rounded-xl p-3 space-y-2">
                  <div className="text-xs font-semibold text-slate-200">최근 태스크</div>
                  {[
                    { w: 'w-40', color: 'bg-success-dark' },
                    { w: 'w-48', color: 'bg-primary-700' },
                  ].map((t, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <div className={`w-3 h-3 rounded ${i === 0 ? 'bg-success' : 'bg-primary-500'}`} />
                      <div className={`h-2 ${t.w} bg-[#475569] rounded`} />
                      <div className="ml-auto">
                        <div className={`h-3.5 w-10 ${t.color} rounded text-[6px] text-white flex items-center justify-center`}>
                          {i === 0 ? 'Done' : 'In Progress'}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
