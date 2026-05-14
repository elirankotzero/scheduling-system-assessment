import { useEffect, useState } from 'react';
import type { ParameterSchema, Scheduling, SchedulingRequest, ScheduleType, TaskType } from '../types';
import { parameterSchemasApi } from '../api/parameterSchemas';
import { tasksApi, type TaskDefinition } from '../api/tasks';
import { DAY_NAMES, scheduleTypeLabel } from '../utils/formatters';
import { X } from 'lucide-react';

interface Props {
  initial?: Scheduling;
  onSubmit: (data: SchedulingRequest) => Promise<void>;
  onClose: () => void;
}

const SCHEDULE_TYPES: ScheduleType[] = [
  'ONE_TIME',
  'RECURRING_MINUTES',
  'RECURRING_HOURS',
  'WEEKLY',
  'CRON',
];

export function ScheduleForm({ initial, onSubmit, onClose }: Props) {
  const isEdit = !!initial;

  const [taskName, setTaskName] = useState(initial?.taskName ?? '');
  const [taskType, setTaskType] = useState<TaskType>(initial?.taskType ?? 'LOG_TASK');
  const [scheduleType, setScheduleType] = useState<ScheduleType>(initial?.scheduleType ?? 'ONE_TIME');
  const [scheduleExpression, setScheduleExpression] = useState(initial?.scheduleExpression ?? '');
  const [dayOfWeek, setDayOfWeek] = useState<number>(initial?.dayOfWeek ?? 1);
  const [timeOfDay, setTimeOfDay] = useState(initial?.timeOfDay ?? '09:00');
  const [scheduledAt, setScheduledAt] = useState(() => {
    if (!initial?.scheduledAt) return '';
    // Backend sends UTC string without 'Z'; add it so Date() parses as UTC,
    // then convert to local time for the datetime-local input.
    const utc = initial.scheduledAt.endsWith('Z') ? initial.scheduledAt : initial.scheduledAt + 'Z';
    const d = new Date(utc);
    const offset = d.getTimezoneOffset() * 60000;
    return new Date(d.getTime() - offset).toISOString().substring(0, 16);
  });
  const [parameters, setParameters] = useState<Record<string, string>>(
    (initial?.parameters as Record<string, string>) ?? {}
  );
  const [taskDefs, setTaskDefs] = useState<TaskDefinition[]>([]);
  const [schemas, setSchemas] = useState<ParameterSchema[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    tasksApi.getAll().then(setTaskDefs).catch(() => setTaskDefs([]));
  }, []);

  useEffect(() => {
    parameterSchemasApi.getByTaskType(taskType).then(setSchemas).catch(() => setSchemas([]));
  }, [taskType]);

  // Reset parameter values when task type changes (not on initial edit load)
  useEffect(() => {
    if (!initial) {
      setParameters({});
    }
  }, [taskType]);

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!taskName.trim()) errs.taskName = 'Task name is required';

    if (scheduleType === 'ONE_TIME' && !scheduledAt) {
      errs.scheduledAt = 'Scheduled date/time is required';
    }
    if ((scheduleType === 'RECURRING_MINUTES' || scheduleType === 'RECURRING_HOURS') && !scheduleExpression) {
      errs.scheduleExpression = 'Interval is required';
    }
    if (scheduleType === 'CRON' && !scheduleExpression) {
      errs.scheduleExpression = 'Cron expression is required';
    }

    for (const schema of schemas.filter((s) => s.required)) {
      if (!parameters[schema.parameterName]?.trim()) {
        errs[`param_${schema.parameterName}`] = `${schema.parameterName} is required`;
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    try {
      const payload: SchedulingRequest = {
        taskName: taskName.trim(),
        taskType,
        scheduleType,
        parameters,
      };

      if (scheduleType === 'ONE_TIME') {
        payload.scheduledAt = new Date(scheduledAt).toISOString();
      } else if (scheduleType === 'RECURRING_MINUTES' || scheduleType === 'RECURRING_HOURS') {
        payload.scheduleExpression = scheduleExpression;
      } else if (scheduleType === 'WEEKLY') {
        payload.dayOfWeek = dayOfWeek;
        payload.timeOfDay = timeOfDay;
      } else if (scheduleType === 'CRON') {
        payload.scheduleExpression = scheduleExpression;
      }

      await onSubmit(payload);
    } finally {
      setSubmitting(false);
    }
  }

  function setParam(name: string, value: string) {
    setParameters((prev) => ({ ...prev, [name]: value }));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">
            {isEdit ? 'Edit Schedule' : 'New Schedule'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {/* Task Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Task Name</label>
            <input
              type="text"
              value={taskName}
              onChange={(e) => setTaskName(e.target.value)}
              placeholder="e.g. Daily Report Log"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.taskName && <p className="text-red-500 text-xs mt-1">{errors.taskName}</p>}
          </div>

          {/* Task Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Task Type</label>
            <select
              value={taskType}
              onChange={(e) => setTaskType(e.target.value as TaskType)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {taskDefs.map((t) => (
                <option key={t.taskType} value={t.taskType}>{t.label}</option>
              ))}
            </select>
          </div>

          {/* Schedule Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Schedule Type</label>
            <select
              value={scheduleType}
              onChange={(e) => setScheduleType(e.target.value as ScheduleType)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {SCHEDULE_TYPES.map((s) => (
                <option key={s} value={s}>{scheduleTypeLabel(s)}</option>
              ))}
            </select>
          </div>

          {/* Conditional schedule fields */}
          {scheduleType === 'ONE_TIME' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Scheduled At</label>
              <input
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.scheduledAt && <p className="text-red-500 text-xs mt-1">{errors.scheduledAt}</p>}
            </div>
          )}

          {(scheduleType === 'RECURRING_MINUTES' || scheduleType === 'RECURRING_HOURS') && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Interval ({scheduleType === 'RECURRING_MINUTES' ? 'minutes' : 'hours'})
              </label>
              <input
                type="number"
                min={1}
                value={scheduleExpression}
                onChange={(e) => setScheduleExpression(e.target.value)}
                placeholder={scheduleType === 'RECURRING_MINUTES' ? 'e.g. 30' : 'e.g. 2'}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.scheduleExpression && (
                <p className="text-red-500 text-xs mt-1">{errors.scheduleExpression}</p>
              )}
            </div>
          )}

          {scheduleType === 'WEEKLY' && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Day of Week</label>
                <select
                  value={dayOfWeek}
                  onChange={(e) => setDayOfWeek(Number(e.target.value))}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {DAY_NAMES.map((day, idx) => (
                    <option key={idx} value={idx}>{day}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Time</label>
                <input
                  type="time"
                  value={timeOfDay}
                  onChange={(e) => setTimeOfDay(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}

          {scheduleType === 'CRON' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Cron Expression</label>
              <input
                type="text"
                value={scheduleExpression}
                onChange={(e) => setScheduleExpression(e.target.value)}
                placeholder="e.g. 0 0 9 ? * MON-FRI"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-gray-400 text-xs mt-1">Quartz cron format: sec min hour day month weekday</p>
              {errors.scheduleExpression && (
                <p className="text-red-500 text-xs mt-1">{errors.scheduleExpression}</p>
              )}
            </div>
          )}

          {/* Dynamic Parameter Fields */}
          {schemas.length > 0 && (
            <div className="border-t pt-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Task Parameters</h3>
              <div className="space-y-3">
                {schemas.map((schema) => (
                  <div key={schema.parameterName}>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      {schema.parameterName}
                      {schema.required && <span className="text-red-500 ml-1">*</span>}
                    </label>
                    {schema.parameterType === 'BOOLEAN' ? (
                      <select
                        value={parameters[schema.parameterName] ?? ''}
                        onChange={(e) => setParam(schema.parameterName, e.target.value)}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="">Select…</option>
                        <option value="true">True</option>
                        <option value="false">False</option>
                      </select>
                    ) : (
                      <input
                        type={schema.parameterType === 'EMAIL' ? 'email' : schema.parameterType === 'INTEGER' ? 'number' : 'text'}
                        value={parameters[schema.parameterName] ?? ''}
                        onChange={(e) => setParam(schema.parameterName, e.target.value)}
                        placeholder={schema.description ?? schema.parameterName}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      />
                    )}
                    {errors[`param_${schema.parameterName}`] && (
                      <p className="text-red-500 text-xs mt-1">
                        {errors[`param_${schema.parameterName}`]}
                      </p>
                    )}
                    {schema.description && (
                      <p className="text-gray-400 text-xs mt-1">{schema.description}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 justify-end pt-2 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 transition font-medium"
            >
              {submitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Schedule'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
