const WEEKDAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

export function formatRecurrence(
  frequency?: string,
  weekday?: number,
  endDate?: string
): string {
  if (!frequency) return ''

  let text = `Repeats ${frequency.toLowerCase()}`

  if (frequency === 'WEEKLY' && weekday !== undefined) {
    text += ` on ${WEEKDAY_NAMES[weekday]}`
  }

  if (endDate) {
    const date = new Date(endDate)
    const formatted = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    text += ` until ${formatted}`
  }

  return text
}

export function getTodayDate(): string {
  const isoString = new Date().toISOString()
  return isoString.substring(0, 10)
}

export const FREQUENCY_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
]
