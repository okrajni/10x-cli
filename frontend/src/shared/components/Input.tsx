import { InputHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: ReactNode
}

export function Input({ label, error, helpText, className, ...props }: InputProps) {
  return (
    <div className="space-y-0">
      {label && (
        <label className="label" htmlFor={props.id}>
          {label}
        </label>
      )}

      <input className={clsx('field', error && 'field-invalid', className)} {...props} />

      {error && (
        <p className="mt-2 flex items-center gap-1.5 pl-1 text-xs font-medium text-accent">
          <span aria-hidden="true">!</span>
          {error}
        </p>
      )}

      {helpText && !error && <p className="hint mt-2 pl-1">{helpText}</p>}
    </div>
  )
}
