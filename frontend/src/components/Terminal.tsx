import React, { useEffect, useRef } from 'react';
import { Terminal as TermIcon, TerminalSquare } from 'lucide-react';

interface LogLine {
  id?: string;
  message: string;
  level: string;
  timestamp: string;
}

interface TerminalProps {
  logs: LogLine[];
}

export const Terminal: React.FC<TerminalProps> = ({ logs }) => {
  const terminalEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll logic
  useEffect(() => {
    terminalEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  const formatTime = (timeStr: string) => {
    try {
      const date = new Date(timeStr);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
    } catch {
      return '';
    }
  };

  return (
    <div className="w-full bg-[#030303] border border-zinc-800 rounded-lg overflow-hidden flex flex-col font-mono text-[13px] text-zinc-300 shadow-2xl">
      {/* Terminal Bar */}
      <div className="bg-[#09090b] border-b border-zinc-800 px-4 py-2.5 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <TerminalSquare className="h-4 w-4 text-zinc-400" />
          <span className="text-xs font-semibold text-zinc-400 select-none">build-logs.log</span>
        </div>
        <div className="flex space-x-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-zinc-800" />
          <div className="w-2.5 h-2.5 rounded-full bg-zinc-800" />
          <div className="w-2.5 h-2.5 rounded-full bg-zinc-800" />
        </div>
      </div>

      {/* Console Display */}
      <div className="p-4 flex-1 h-[400px] overflow-y-auto space-y-1.5 select-text selection:bg-zinc-700 selection:text-white">
        {logs.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-zinc-600 select-none">
            <TermIcon className="h-8 w-8 mb-2 animate-pulse" />
            <p>Initializing runtime environment...</p>
            <p className="text-[11px] mt-1">Waiting for deployment execution logs</p>
          </div>
        ) : (
          logs.map((log, idx) => {
            let color = 'text-zinc-300';
            if (log.level === 'WARN') color = 'text-amber-400';
            if (log.level === 'ERROR') color = 'text-rose-400';

            return (
              <div key={log.id || idx} className="flex items-start leading-normal transition-all hover:bg-zinc-900/40 px-1 rounded">
                <span className="text-zinc-600 text-xs w-16 select-none shrink-0 pr-2">
                  [{formatTime(log.timestamp)}]
                </span>
                <span className="text-zinc-500 text-xs w-10 select-none shrink-0 pr-2 font-semibold">
                  {log.level}
                </span>
                <span className={`${color} whitespace-pre-wrap break-all flex-1`}>
                  {log.message}
                </span>
              </div>
            );
          })
        )}
        <div ref={terminalEndRef} />
      </div>
    </div>
  );
};
