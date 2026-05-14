import { useCallback, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { schedulesApi } from '../api/schedules';
import type { Scheduling, SchedulingRequest } from '../types';
import { ScheduleTable } from './ScheduleTable';
import { ScheduleForm } from './ScheduleForm';
import { ConfirmDialog } from './ConfirmDialog';
import { Plus, RefreshCw, CalendarClock } from 'lucide-react';

export function Dashboard() {
  const [schedules, setSchedules] = useState<Scheduling[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState<Scheduling | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<Scheduling | undefined>();

  const fetchAll = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const data = await schedulesApi.getAll();
      setSchedules(data);
    } catch {
      if (!silent) toast.error('Failed to load schedules');
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  // Auto-refresh every 30 seconds (silent — no spinner or toast)
  useEffect(() => {
    const interval = setInterval(() => fetchAll(true), 30_000);
    return () => clearInterval(interval);
  }, [fetchAll]);

  async function handleCreate(data: SchedulingRequest) {
    await schedulesApi.create(data);
    toast.success('Schedule created');
    setShowForm(false);
    fetchAll();
  }

  async function handleUpdate(data: SchedulingRequest) {
    if (!editTarget) return;
    await schedulesApi.update(editTarget.id, data);
    toast.success('Schedule updated');
    setEditTarget(undefined);
    fetchAll();
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    await schedulesApi.delete(deleteTarget.id);
    toast.success('Schedule deleted');
    setDeleteTarget(undefined);
    fetchAll();
  }

  async function handlePause(s: Scheduling) {
    try {
      await schedulesApi.pause(s.id);
      toast.success(`"${s.taskName}" paused`);
      fetchAll();
    } catch {
      toast.error('Failed to pause schedule');
    }
  }

  async function handleResume(s: Scheduling) {
    try {
      await schedulesApi.resume(s.id);
      toast.success(`"${s.taskName}" resumed`);
      fetchAll();
    } catch {
      toast.error('Failed to resume schedule');
    }
  }

  const activeCount = schedules.filter((s) => s.status === 'ACTIVE').length;

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Top bar */}
      <header className="bg-white border-b shadow-sm">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <CalendarClock className="text-blue-600" size={26} />
            <div>
              <h1 className="text-xl font-bold text-gray-900">Scheduling System</h1>
              <p className="text-xs text-gray-400">Manage your scheduled tasks</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-500">
              {activeCount} active · {schedules.length} total
            </span>
            <button
              onClick={() => fetchAll()}
              title="Refresh"
              className="p-2 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition"
            >
              <RefreshCw size={16} />
            </button>
            <button
              onClick={() => setShowForm(true)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition shadow-sm"
            >
              <Plus size={16} />
              New Schedule
            </button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="max-w-7xl mx-auto px-6 py-6">
        {/* Stats row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
          {(
            [
              { label: 'Total', value: schedules.length, color: 'bg-gray-100 text-gray-800' },
              { label: 'Active', value: schedules.filter(s => s.status === 'ACTIVE').length, color: 'bg-green-50 text-green-800' },
              { label: 'Paused', value: schedules.filter(s => s.status === 'PAUSED').length, color: 'bg-yellow-50 text-yellow-800' },
              { label: 'Completed', value: schedules.filter(s => s.status === 'COMPLETED').length, color: 'bg-blue-50 text-blue-800' },
            ] as const
          ).map(({ label, value, color }) => (
            <div key={label} className={`rounded-xl p-4 ${color}`}>
              <p className="text-2xl font-bold">{value}</p>
              <p className="text-xs font-medium mt-0.5">{label}</p>
            </div>
          ))}
        </div>

        {/* Table card */}
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <div className="px-5 py-4 border-b">
            <h2 className="font-semibold text-gray-900 text-sm">All Schedules</h2>
          </div>
          <ScheduleTable
            schedules={schedules}
            loading={loading}
            onEdit={(s) => setEditTarget(s)}
            onDelete={(s) => setDeleteTarget(s)}
            onPause={handlePause}
            onResume={handleResume}
          />
        </div>
      </main>

      {/* Modals */}
      {showForm && (
        <ScheduleForm
          onSubmit={handleCreate}
          onClose={() => setShowForm(false)}
        />
      )}

      {editTarget && (
        <ScheduleForm
          initial={editTarget}
          onSubmit={handleUpdate}
          onClose={() => setEditTarget(undefined)}
        />
      )}

      {deleteTarget && (
        <ConfirmDialog
          message={`Delete schedule "${deleteTarget.taskName}"? This will also remove the Quartz job.`}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(undefined)}
        />
      )}
    </div>
  );
}
