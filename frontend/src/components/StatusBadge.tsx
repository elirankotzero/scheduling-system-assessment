import type { ScheduleStatus } from '../types';
import { statusColor } from '../utils/formatters';

interface Props {
  status: ScheduleStatus;
}

export function StatusBadge({ status }: Props) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusColor(status)}`}>
      {status}
    </span>
  );
}
