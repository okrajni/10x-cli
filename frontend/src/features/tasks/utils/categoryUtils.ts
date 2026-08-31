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
    CLEANING: 'bg-green-50 text-green-700',
    SHOPPING: 'bg-rose-50 text-rose-200',
    LAUNDRY: 'bg-cream-100 text-charcoal',
    MAINTENANCE: 'bg-orange-100 text-orange-700',
    BILLS: 'bg-rose-50 text-rose-200',
  }
  return colors[category]
}

export const CATEGORIES: TaskCategory[] = ['CLEANING', 'SHOPPING', 'LAUNDRY', 'MAINTENANCE', 'BILLS']
