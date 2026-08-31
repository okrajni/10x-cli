export function validateRecurrence(frequency?: string, weekday?: number): string | null {
  if (frequency === 'WEEKLY' && weekday === undefined) {
    return 'Weekday is required for weekly recurrence'
  }
  return null
}
