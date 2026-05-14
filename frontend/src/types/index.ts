export type TaskType = 'LOG_TASK' | 'EMAIL_TASK';

export type ScheduleType =
  | 'ONE_TIME'
  | 'RECURRING_MINUTES'
  | 'RECURRING_HOURS'
  | 'WEEKLY'
  | 'CRON';

export type ScheduleStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ERROR';

export type ParameterType = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'EMAIL';

export interface ParameterSchema {
  id: number;
  taskType: TaskType;
  parameterName: string;
  parameterType: ParameterType;
  required: boolean;
  description?: string;
  defaultValue?: string;
}

export interface Scheduling {
  id: number;
  taskName: string;
  taskType: TaskType;
  scheduleType: ScheduleType;
  scheduleExpression?: string;
  dayOfWeek?: number;
  timeOfDay?: string;
  scheduledAt?: string;
  parameters?: Record<string, string>;
  status: ScheduleStatus;
  quartzJobKey?: string;
  createdAt: string;
  updatedAt: string;
  lastExecutedAt?: string;
  nextExecutionAt?: string;
}

export interface SchedulingRequest {
  taskName: string;
  taskType: TaskType;
  scheduleType: ScheduleType;
  scheduleExpression?: string;
  dayOfWeek?: number;
  timeOfDay?: string;
  scheduledAt?: string;
  parameters?: Record<string, string>;
}
