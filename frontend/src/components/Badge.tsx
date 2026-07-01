import React from 'react';

interface BadgeProps {
  status: string;
}

export const Badge: React.FC<BadgeProps> = ({ status }) => {
  let colorClass = 'bg-zinc-800 text-zinc-400 border-zinc-700';

  switch (status) {
    case 'QUEUED':
      colorClass = 'bg-zinc-900 text-zinc-400 border-zinc-800';
      break;
    case 'BUILDING':
      colorClass = 'bg-blue-950/40 text-blue-400 border-blue-900/50';
      break;
    case 'DEPLOYING':
      colorClass = 'bg-amber-950/40 text-amber-400 border-amber-900/50';
      break;
    case 'RUNNING':
      colorClass = 'bg-emerald-950/40 text-emerald-400 border-emerald-900/50';
      break;
    case 'FAILED':
      colorClass = 'bg-rose-950/40 text-rose-400 border-rose-900/50';
      break;
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-mono border ${colorClass}`}>
      {status}
    </span>
  );
};
