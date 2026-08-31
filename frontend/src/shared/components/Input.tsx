import { InputHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: ReactNode
}

export function Input({ label, error, helpText, className, ...props }: InputProps) {
  const inputStyles = clsx(
    'px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-600 focus:border-green-600 transition',
    error ? 'border-rose-100 bg-rose-50' : 'border-gray-300'
  )

  return (
    <div>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <input className={clsx(inputStyles, className)} {...props} />
      {error && <p className="mt-1 text-sm text-rose-200">{error}</p>}
      {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
    </div>
  )
}
