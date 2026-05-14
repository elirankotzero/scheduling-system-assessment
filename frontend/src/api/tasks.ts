import client from './client';
import type { TaskType } from '../types';

export interface TaskDefinition {
  taskType: TaskType;
  label: string;
  description: string;
}

export const tasksApi = {
  getAll: () =>
    client.get<TaskDefinition[]>('/api/tasks').then((r) => r.data),
};
