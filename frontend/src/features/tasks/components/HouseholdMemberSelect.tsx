import { HouseholdMember } from '@features/household/api'

interface HouseholdMemberSelectProps {
  members: HouseholdMember[]
  value: string | null | undefined
  onChange: (id: string) => void
  label?: string
  required?: boolean
  disabled?: boolean
}

export function HouseholdMemberSelect({
  members,
  value,
  onChange,
  label = 'Assign to',
  required = false,
  disabled = false,
}: HouseholdMemberSelectProps) {
  return (
    <div>
      <label htmlFor="assignee" className="block text-sm font-medium text-gray-700 mb-2">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>
      <select
        id="assignee"
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <option value="">Select an assignee</option>
        {members.map((member) => (
          <option key={member.id} value={member.id}>
            {member.name} ({member.email})
          </option>
        ))}
      </select>
    </div>
  )
}
