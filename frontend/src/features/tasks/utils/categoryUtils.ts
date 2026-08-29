import { TaskCategory } from '../api'

export function categoryLabel(category: TaskCategory): string {
  const labels: Record<TaskCategory, string> = {
    CLEANING: 'Cleaning',
    SHOPPING: 'Shopping',
    LAUNDRY: 'Laundry',
    MAINTENANCE: 'Maintenance',
    BILLS: 'Bills',
  }
  return labels[category]
}

export function categoryColor(category: TaskCategory): string {
  const colors: Record<TaskCategory, string> = {
    CLEANING: 'bg-blue-100 text-blue-800',
    SHOPPING: 'bg-green-100 text-green-800',
    LAUNDRY: 'bg-purple-100 text-purple-800',
    MAINTENANCE: 'bg-orange-100 text-orange-800',
    BILLS: 'bg-red-100 text-red-800',
  }
  return colors[category]
}

export const CATEGORIES: TaskCategory[] = ['CLEANING', 'SHOPPING', 'LAUNDRY', 'MAINTENANCE', 'BILLS']
