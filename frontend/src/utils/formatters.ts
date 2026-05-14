import type { ScheduleStatus, ScheduleType, TaskType } from '../types';

export function formatDate(dateStr?: string): string {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString();
}

export function taskTypeLabel(t: TaskType): string {
  return { LOG_TASK: 'Log Task', EMAIL_TASK: 'Email Task' }[t] ?? t;
}

export function scheduleTypeLabel(s: ScheduleType): string {
  return {
    ONE_TIME: 'One-time',
    RECURRING_MINUTES: 'Every X minutes',
    RECURRING_HOURS: 'Every X hours',
    WEEKLY: 'Weekly',
    CRON: 'Cron expression',
  }[s] ?? s;
}

export function statusColor(status: ScheduleStatus): string {
  return {
    ACTIVE:    'bg-green-100 text-green-800',
    PAUSED:    'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-blue-100 text-blue-800',
    ERROR:     'bg-red-100 text-red-800',
  }[status] ?? 'bg-gray-100 text-gray-800';
}

export const DAY_NAMES = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
