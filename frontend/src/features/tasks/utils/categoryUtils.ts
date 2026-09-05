import { TaskCategory } from '../api'

export function categoryLabel(category: TaskCategory): string {
  const labels: Record<TaskCategory, string> = {
    CLEANING: 'Cleaning',
    SHOPPING: 'Shopping',
    LAUNDRY: 'Laundry',
    MAINTENANCE: 'Maintenance',
    BILLS: 'Bills',
    ERRANDS: 'Errands',
    SEASONAL: 'Seasonal',
  }
  return labels[category]
}

/**
 * Categories are told apart by how much accent fills the chip — not by hue.
 * Every step stays inside the two-color palette (accent over canvas), which
 * keeps the badges legible without introducing a third or fourth color.
 */
export function categoryColor(category: TaskCategory): string {
  const fills: Record<TaskCategory, string> = {
    CLEANING: 'border-accent/40 bg-transparent text-accent',
    SHOPPING: 'border-accent/40 bg-accent/10 text-accent',
    LAUNDRY: 'border-accent/50 bg-accent/20 text-accent',
    MAINTENANCE: 'border-accent/70 bg-accent/30 text-accent',
    BILLS: 'border-accent bg-accent text-canvas',
    ERRANDS: 'border-accent/35 bg-accent/5 text-accent',
    SEASONAL: 'border-accent/60 bg-accent/25 text-accent',
  }
  return fills[category]
}

export const CATEGORIES: TaskCategory[] = ['CLEANING', 'SHOPPING', 'LAUNDRY', 'MAINTENANCE', 'BILLS', 'ERRANDS', 'SEASONAL']
