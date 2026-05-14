import client from './client';
import type { Scheduling, SchedulingRequest } from '../types';

export const schedulesApi = {
  getAll: () =>
    client.get<Scheduling[]>('/api/schedules').then((r) => r.data),

  getById: (id: number) =>
    client.get<Scheduling>(`/api/schedules/${id}`).then((r) => r.data),

  create: (data: SchedulingRequest) =>
    client.post<Scheduling>('/api/schedules', data).then((r) => r.data),

  update: (id: number, data: SchedulingRequest) =>
    client.put<Scheduling>(`/api/schedules/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    client.delete(`/api/schedules/${id}`),

  pause: (id: number) =>
    client.post<Scheduling>(`/api/schedules/${id}/pause`).then((r) => r.data),

  resume: (id: number) =>
    client.post<Scheduling>(`/api/schedules/${id}/resume`).then((r) => r.data),
};
