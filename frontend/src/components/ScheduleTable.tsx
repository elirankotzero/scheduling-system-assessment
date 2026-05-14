import type { Scheduling } from '../types';
import { StatusBadge } from './StatusBadge';
import {
  formatDate,
  scheduleTypeLabel,
  taskTypeLabel,
} from '../utils/formatters';
import { Edit2, Trash2, Pause, Play } from 'lucide-react';

interface Props {
  schedules: Scheduling[];
  loading: boolean;
  onEdit: (s: Scheduling) => void;
  onDelete: (s: Scheduling) => void;
  onPause: (s: Scheduling) => void;
  onResume: (s: Scheduling) => void;
}

export function ScheduleTable({
  schedules,
  loading,
  onEdit,
  onDelete,
  onPause,
  onResume,
}: Props) {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-24 text-gray-400 text-sm">
        Loading schedules…
      </div>
    );
  }

  if (schedules.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-gray-400">
        <p className="text-lg font-medium mb-1">No schedules yet</p>
        <p className="text-sm">Click "New Schedule" to create one.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm text-left">
        <thead>
          <tr className="border-b bg-gray-50 text-gray-500 uppercase text-xs tracking-wide">
            <th className="px-4 py-3 font-medium">ID</th>
            <th className="px-4 py-3 font-medium">Name</th>
            <th className="px-4 py-3 font-medium">Task</th>
            <th className="px-4 py-3 font-medium">Schedule</th>
            <th className="px-4 py-3 font-medium">Status</th>
            <th className="px-4 py-3 font-medium">Next Run</th>
            <th className="px-4 py-3 font-medium">Last Run</th>
            <th className="px-4 py-3 font-medium text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {schedules.map((s) => (
            <tr key={s.id} className="hover:bg-gray-50 transition-colors">
              <td className="px-4 py-3 text-gray-400 font-mono">{s.id}</td>
              <td className="px-4 py-3 font-medium text-gray-900">{s.taskName}</td>
              <td className="px-4 py-3 text-gray-600">{taskTypeLabel(s.taskType)}</td>
              <td className="px-4 py-3 text-gray-600">
                <div>{scheduleTypeLabel(s.scheduleType)}</div>
                {s.scheduleExpression && (
                  <div className="text-xs text-gray-400 font-mono">{s.scheduleExpression}</div>
                )}
              </td>
              <td className="px-4 py-3">
                <StatusBadge status={s.status} />
              </td>
              <td className="px-4 py-3 text-gray-500 text-xs">{formatDate(s.nextExecutionAt)}</td>
              <td className="px-4 py-3 text-gray-500 text-xs">{formatDate(s.lastExecutedAt)}</td>
              <td className="px-4 py-3">
                <div className="flex items-center gap-2 justify-end">
                  {s.status === 'ACTIVE' ? (
                    <button
                      onClick={() => onPause(s)}
                      title="Pause"
                      className="p-1.5 rounded-lg text-yellow-600 hover:bg-yellow-50 transition"
                    >
                      <Pause size={15} />
                    </button>
                  ) : s.status === 'PAUSED' ? (
                    <button
                      onClick={() => onResume(s)}
                      title="Resume"
                      className="p-1.5 rounded-lg text-green-600 hover:bg-green-50 transition"
                    >
                      <Play size={15} />
                    </button>
                  ) : null}

                  <button
                    onClick={() => onEdit(s)}
                    title="Edit"
                    className="p-1.5 rounded-lg text-blue-600 hover:bg-blue-50 transition"
                  >
                    <Edit2 size={15} />
                  </button>
                  <button
                    onClick={() => onDelete(s)}
                    title="Delete"
                    className="p-1.5 rounded-lg text-red-600 hover:bg-red-50 transition"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
