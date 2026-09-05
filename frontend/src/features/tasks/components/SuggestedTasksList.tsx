import { useEffect, useState } from 'react'
import clsx from 'clsx'
import { Button, Notice, Spinner } from '@shared/components'
import {
  getSuggestions,
  createTask,
  DomainTask,
  logSuggestionInteraction,
  type TaskCategory,
} from '../api'
import { categoryColor, categoryLabel } from '../utils/categoryUtils'

interface SuggestedTasksListProps {
  householdId?: string
  onSuggestionAccepted?: () => void
}

export function SuggestedTasksList({ onSuggestionAccepted }: SuggestedTasksListProps) {
  const [suggestions, setSuggestions] = useState<DomainTask[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionInProgress, setActionInProgress] = useState<string | null>(null)

  console.log('SuggestedTasksList render - suggestions count:', suggestions.length)

  useEffect(() => {
    fetchSuggestions()
  }, [])

  const fetchSuggestions = async () => {
    setIsLoading(true)
    setError(null)

    try {
      const result = await getSuggestions()

      if (result.ok) {
        setSuggestions(result.data.suggestions)
      } else {
        setError(result.message || 'Failed to fetch suggestions')
      }
    } catch {
      setError('An error occurred while fetching suggestions')
    } finally {
      setIsLoading(false)
    }
  }

  const handleAccept = async (suggestion: DomainTask) => {
    setActionInProgress(suggestion.id)

    try {
      const today = new Date().toISOString().split('T')[0] ?? ''
      console.log('Creating task:', { title: suggestion.title, category: suggestion.category, dueDate: today })

      if (!suggestion.title || !suggestion.category) {
        throw new Error('Invalid suggestion data')
      }

      const result = await createTask({
        title: suggestion.title,
        category: suggestion.category as TaskCategory,
        dueDate: today,
      })

      console.log('Task creation result:', result)

      if (result.ok) {
        console.log('Success! Removing suggestion from list')
        // Remove from suggestions immediately
        const filtered = suggestions.filter((s) => s.id !== suggestion.id)
        console.log('Suggestions before:', suggestions.length, 'after:', filtered.length)
        setSuggestions(filtered)

        // Notify parent to refresh tasks
        onSuggestionAccepted?.()

        // Log the interaction (Phase 3 - optional, fire-and-forget)
        if (suggestion.id) {
          logSuggestionInteraction({
            suggestionId: suggestion.id,
            action: 'accepted',
          }).catch(() => {
            // Silent failure - Phase 3 logging endpoint may not exist yet
          })
        }
      } else {
        console.error('Task creation failed:', result)
        setError(result.message || 'Failed to accept suggestion')
      }
    } catch (error) {
      console.error('Error accepting suggestion:', error)
      setError('An error occurred while accepting the suggestion')
    } finally {
      setActionInProgress(null)
    }
  }

  const handleDismiss = async (suggestion: DomainTask) => {
    // Remove from local state optimistically
    setSuggestions(suggestions.filter((s) => s.id !== suggestion.id))

    // Log the interaction (fire-and-forget)
    logSuggestionInteraction({
      suggestionId: suggestion.id,
      action: 'dismissed',
    }).catch(() => {
      // Silent failure for analytics logging
    })
  }

  if (isLoading) {
    return (
      <div className="py-6 text-center">
        <Spinner />
        <p className="mt-2 text-xs font-light text-accent/70">Loading suggestions…</p>
      </div>
    )
  }

  if (suggestions.length === 0) {
    return null
  }

  return (
    <>
      {/* Divider */}
      <div className="my-6 border-t border-accent/20" />

      {/* Suggestions Section */}
      <div>
        <h3 className="mb-4 text-sm font-semibold text-accent">Suggested Tasks</h3>

        {error && (
          <Notice tone="error" className="mb-4" onDismiss={() => setError(null)}>
            {error}
            <button
              onClick={fetchSuggestions}
              className="ml-2 text-xs font-medium underline hover:no-underline"
            >
              Retry
            </button>
          </Notice>
        )}

        <div className="space-y-2">
          {suggestions.map((suggestion) => (
            <div
              key={suggestion.id}
              className={clsx(
                'rounded-card border border-accent/20 bg-accent/[0.02] p-4 transition',
                'hover:border-accent/40'
              )}
            >
              <div className="mb-3 flex items-start justify-between gap-4">
                <div className="flex-1">
                  <h4 className="font-serif text-sm font-semibold text-accent">
                    {suggestion.title}
                  </h4>
                  {suggestion.frequencyDays && (
                    <p className="mt-1 text-xs font-light text-accent/60">
                      Every {suggestion.frequencyDays} days
                    </p>
                  )}
                </div>

                <span
                  className={clsx(
                    'shrink-0 rounded-pill border px-2 py-0.5 text-[0.65rem] font-medium uppercase tracking-label',
                    categoryColor(suggestion.category)
                  )}
                >
                  {categoryLabel(suggestion.category)}
                </span>
              </div>

              <div className="flex flex-wrap justify-end gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handleDismiss(suggestion)}
                  disabled={actionInProgress === suggestion.id}
                  aria-label={`Dismiss ${suggestion.title}`}
                >
                  Dismiss
                </Button>

                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => handleAccept(suggestion)}
                  disabled={actionInProgress === suggestion.id}
                  aria-label={`Accept ${suggestion.title}`}
                >
                  {actionInProgress === suggestion.id ? 'Adding…' : 'Accept'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
