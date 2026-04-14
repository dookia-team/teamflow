import { GoogleLoginButton } from '@/features/auth'

export function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white rounded-card shadow-lg p-8 md:p-10 w-full max-w-[420px] mx-4 text-center">
        <h1 className="text-[28px] font-bold text-gray-900 mb-2">
          Team<span className="text-primary-600">Flow</span>
        </h1>
        <p className="text-gray-500 mb-8">로그인하여 팀과 함께하세요</p>
        <div className="flex justify-center">
          <GoogleLoginButton />
        </div>
      </div>
    </div>
  )
}
