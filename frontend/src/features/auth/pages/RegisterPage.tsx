import { Link } from 'react-router-dom'
import { Button } from '@shared/components'

export default function RegisterPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-lg p-8 space-y-6">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">done yet?</h1>
            <p className="text-gray-600 mt-2">Create your account</p>
          </div>

          <p className="text-center text-sm text-gray-600">
            Registration will be fully implemented in S-01. For now, use login with any email/password.
          </p>

          <Button variant="primary" disabled className="w-full">
            Sign Up (Coming Soon)
          </Button>

          <p className="text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 hover:underline font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
