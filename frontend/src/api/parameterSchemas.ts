import client from './client';
import type { ParameterSchema, TaskType } from '../types';

export const parameterSchemasApi = {
  getAll: () =>
    client.get<ParameterSchema[]>('/api/parameter-schemas').then((r) => r.data),

  getByTaskType: (taskType: TaskType) =>
    client.get<ParameterSchema[]>(`/api/parameter-schemas/task/${taskType}`).then((r) => r.data),
};
