import { useState } from 'react'
import { Button, Input, Dialog, TaskCard } from '@shared/components'
import { Task } from '@features/tasks/api'

// Component showcase for Phase 4 testing
export default function App() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [email, setEmail] = useState('')

  const mockTask: Task = {
    id: '1',
    title: 'Buy groceries',
    description: 'Milk, eggs, bread',
    category: 'shopping',
    dueDate: new Date(Date.now() + 86400000).toISOString(),
    assignedTo: 'me',
    status: 'pending',
    householdId: 'h1',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        <h1 className="text-3xl font-bold">Component Library</h1>

        {/* Buttons */}
        <section>
          <h2 className="text-xl font-bold mb-4">Buttons</h2>
          <div className="flex gap-4">
            <Button variant="primary">Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="danger">Danger</Button>
            <Button disabled>Disabled</Button>
          </div>
        </section>

        {/* Inputs */}
        <section>
          <h2 className="text-xl font-bold mb-4">Inputs</h2>
          <div className="space-y-4 max-w-xs">
            <Input
              label="Email"
              type="email"
              placeholder="test@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Input label="Email with Error" error="Email is required" />
            <Input
              label="Email with Help"
              helpText="We'll never share your email"
              type="email"
            />
          </div>
        </section>

        {/* Dialog */}
        <section>
          <h2 className="text-xl font-bold mb-4">Dialog</h2>
          <Button onClick={() => setDialogOpen(true)}>Open Dialog</Button>

          <Dialog open={dialogOpen} onOpenChange={setDialogOpen} title="Example Dialog">
            <p>This is a dialog component using Radix UI and Tailwind CSS.</p>
          </Dialog>
        </section>

        {/* Task Card */}
        <section>
          <h2 className="text-xl font-bold mb-4">Task Card</h2>
          <TaskCard
            task={mockTask}
            onComplete={(id) => console.log('Complete:', id)}
            onEdit={(task) => console.log('Edit:', task)}
            onDelete={(id) => console.log('Delete:', id)}
          />
        </section>
      </div>
    </div>
  )
}
