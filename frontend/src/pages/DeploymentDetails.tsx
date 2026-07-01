import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { Badge } from '../components/Badge';
import { Terminal } from '../components/Terminal';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { 
  ArrowLeft, 
  Clock, 
  ExternalLink, 
  GitCommit, 
  Loader2, 
  Server,
  Terminal as TermIcon,
  Cpu,
  TrendingUp,
  Activity,
  History,
  AlertOctagon
} from 'lucide-react';

interface Deployment {
  id: string;
  projectId: string;
  projectName: string;
  status: string;
  deploymentUrl: string;
  startedAt: string;
  completedAt: string;
  gitCommitHash: string;
  versionTag: string;
  
  // V3 Metadata
  containerId?: string;
  containerName?: string;
  imageTag?: string;
  hostPort?: number;
  buildDurationMs?: number;
  frameworkDetected?: string;
  triggerType?: string;
  githubCommitHash?: string;
  githubCommitMessage?: string;
  githubAuthor?: string;
  deploymentDurationMs?: number;
  imageSizeMb?: number;
  failureStage?: string;
  failureSummary?: string;
}

interface LogLine {
  id: string;
  deploymentId: string;
  message: string;
  level: string;
  timestamp: string;
}

interface TimelineEvent {
  eventType: string;
  message: string;
  createdAt: string;
}

interface Metrics {
  cpuUsagePercent: number;
  memoryUsageMb: number;
  uptimeSeconds: number;
}

export const DeploymentDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [deployment, setDeployment] = useState<Deployment | null>(null);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [metrics, setMetrics] = useState<Metrics | null>(null);

  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'logs' | 'metrics' | 'timeline'>('logs');

  const fetchLogsAndStatus = async () => {
    try {
      const [depRes, logRes, eventRes] = await Promise.all([
        apiClient.get(`/deployments/${id}`),
        apiClient.get(`/logs/${id}`),
        apiClient.get(`/deployments/${id}/events`)
      ]);
      if (depRes.data.success) setDeployment(depRes.data.data);
      if (logRes.data.success) setLogs(logRes.data.data);
      if (eventRes.data.success) setEvents(eventRes.data.data);
    } catch (err) {
      console.error('Failed to update deployment logs/status', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchMetrics = async () => {
    if (!deployment || deployment.status !== 'RUNNING') return;
    try {
      const res = await apiClient.get(`/deployments/${id}/metrics`);
      if (res.data.success) {
        setMetrics(res.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch container metrics', err);
    }
  };

  // 1. Initial REST fetch + Polling fallback for status updates
  useEffect(() => {
    fetchLogsAndStatus();
  }, [id]);

  // 2. Metrics Polling Loop (Only if RUNNING)
  useEffect(() => {
    if (deployment?.status === 'RUNNING') {
      fetchMetrics();
      const interval = setInterval(fetchMetrics, 5000);
      return () => clearInterval(interval);
    }
  }, [id, deployment?.status]);

  // 3. WebSocket / STOMP Log and Status Stream Subscriptions
  useEffect(() => {
    const wsUrl = `${window.location.origin.replace('3000', '8080')}/ws`;
    const socket = new SockJS(wsUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to live log streaming
        client.subscribe(`/topic/deployments/${id}/logs`, (message) => {
          const body = JSON.parse(message.body);
          if (body.type === 'LOG') {
            const mappedLog: LogLine = {
              id: Math.random().toString(),
              deploymentId: id || '',
              message: body.message,
              level: 'INFO',
              timestamp: body.timestamp
            };
            setLogs(prev => [...prev, mappedLog]);
          }
        });

        // Subscribe to live status updates
        client.subscribe(`/topic/deployments/${id}/status`, (message) => {
          const body = JSON.parse(message.body);
          if (body.type === 'STATUS') {
            // Trigger complete status and timeline refresh
            fetchLogsAndStatus();
          }
        });
      },
      onStompError: (frame) => {
        console.error('STOMP Connection error: ' + frame.body);
      }
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [id]);

  const getDuration = (start: string, end: string) => {
    if (!start) return '-';
    const endTime = end ? new Date(end).getTime() : new Date().getTime();
    const diff = endTime - new Date(start).getTime();
    const sec = Math.round(diff / 1000);
    if (sec < 60) return `${sec}s`;
    return `${Math.round(sec / 60)}m ${sec % 60}s`;
  };

  const formatUptime = (seconds: number) => {
    if (!seconds) return '0s';
    if (seconds < 60) return `${seconds}s`;
    const min = Math.floor(seconds / 60);
    const hrs = Math.floor(min / 60);
    if (hrs > 0) return `${hrs}h ${min % 60}m`;
    return `${min}m ${seconds % 60}s`;
  };

  if (loading && !deployment) {
    return (
      <Layout>
        <div className="h-[60vh] flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      </Layout>
    );
  }

  if (!deployment) {
    return (
      <Layout>
        <div className="text-center py-12">
          <AlertOctagon className="h-10 w-10 text-rose-500 mx-auto mb-3" />
          <h2 className="text-lg font-bold text-white">Deployment Not Found</h2>
          <p className="text-xs text-muted-foreground mt-1 mb-5">
            This deployment run does not exist or you do not have permission to view it.
          </p>
          <Link to="/" className="text-primary hover:underline text-xs">Return to Dashboard</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-8">
        {/* Navigation back */}
        <div className="border-b border-border pb-4">
          <Link 
            to={`/project/${deployment.projectId}`} 
            className="inline-flex items-center space-x-1.5 text-xs text-muted-foreground hover:text-white transition-colors cursor-pointer"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Project: {deployment.projectName}</span>
          </Link>
        </div>

        {/* Diagnostic Error Banner if pipeline crashed */}
        {deployment.failureSummary && (
          <div className="bg-rose-950/20 border border-rose-900/30 p-5 rounded-xl flex items-start space-x-3.5">
            <AlertOctagon className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h3 className="text-xs font-bold text-rose-400 uppercase tracking-wide">
                Build Pipeline Failed: {deployment.failureStage}
              </h3>
              <p className="text-xs text-zinc-400 leading-relaxed font-mono select-all">
                {deployment.failureSummary}
              </p>
            </div>
          </div>
        )}

        {/* Metadata Details Card */}
        <div className="bg-card border border-border p-6 rounded-xl flex flex-col justify-between gap-6 shadow-sm">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <h1 className="text-lg font-bold text-white font-mono select-all">
                  dep-{deployment.id.substring(0, 8)}
                </h1>
                <Badge status={deployment.status} />
              </div>
              
              {/* Spinning progress indicators */}
              {(deployment.status === 'QUEUED' || deployment.status === 'CLONING' || deployment.status === 'BUILDING' || deployment.status === 'CREATING_IMAGE' || deployment.status === 'STARTING_CONTAINER') && (
                <div className="flex items-center space-x-2 text-zinc-400 text-xs font-medium">
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                  <span>Deployment in progress (WebSockets Streaming)...</span>
                </div>
              )}
            </div>

            <div className="flex flex-wrap gap-x-6 gap-y-2 text-[11px] text-zinc-500 font-mono">
              <span className="flex items-center space-x-1">
                <Clock className="h-3.5 w-3.5 text-zinc-600" />
                <span>Started {new Date(deployment.startedAt).toLocaleString()}</span>
              </span>
              <span className="flex items-center space-x-1">
                <Server className="h-3.5 w-3.5 text-zinc-600" />
                <span>Duration: {getDuration(deployment.startedAt, deployment.completedAt)}</span>
              </span>
              <span className="flex items-center space-x-1">
                <GitCommit className="h-3.5 w-3.5 text-zinc-600" />
                <span>Commit: {deployment.gitCommitHash ? deployment.gitCommitHash.substring(0, 7) : 'HEAD'}</span>
              </span>
              {deployment.buildDurationMs && (
                <span className="flex items-center space-x-1">
                  <TermIcon className="h-3.5 w-3.5 text-zinc-600" />
                  <span>Compiled: {Math.round(deployment.buildDurationMs / 1000)}s</span>
                </span>
              )}
              {deployment.frameworkDetected && (
                <span className="flex items-center space-x-1">
                  <Badge status={deployment.frameworkDetected} />
                </span>
              )}
            </div>

            {/* Container details card if active */}
            {deployment.containerId && (
              <div className="border-t border-border/60 pt-4 mt-2 grid grid-cols-2 md:grid-cols-5 gap-4 text-[10px] text-zinc-400 font-mono">
                <div>
                  <span className="text-zinc-500">Container ID:</span> <span className="text-zinc-300 font-semibold">{deployment.containerId.substring(0, 12)}</span>
                </div>
                <div>
                  <span className="text-zinc-500">Container Name:</span> <span className="text-zinc-300 font-semibold truncate block max-w-[120px]">{deployment.containerName}</span>
                </div>
                <div>
                  <span className="text-zinc-500">Host Port:</span> <span className="text-zinc-300 font-semibold">{deployment.hostPort}</span>
                </div>
                {deployment.imageSizeMb && (
                  <div>
                    <span className="text-zinc-500">Image Size:</span> <span className="text-zinc-300 font-semibold">{Math.round(deployment.imageSizeMb)} MB</span>
                  </div>
                )}
                <div>
                  <span className="text-zinc-500">Trigger:</span> <span className="text-zinc-300 font-semibold">{deployment.triggerType === 'GITHUB_WEBHOOK' ? 'GitHub Webhook' : 'Manual'}</span>
                </div>
              </div>
            )}

            {deployment.status === 'RUNNING' && deployment.deploymentUrl && (
              <div className="pt-2 flex flex-wrap gap-3 items-center">
                <a
                  href={deployment.deploymentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center space-x-1.5 text-xs font-semibold text-zinc-400 hover:text-zinc-300 transition-colors border border-zinc-800 bg-zinc-900/40 px-3 py-1.5 rounded-lg"
                >
                  <span>Open URL</span>
                  <ExternalLink className="h-3.5 w-3.5" />
                </a>
                <Link
                  to={`/deployment/${deployment.id}/preview`}
                  className="inline-flex items-center space-x-1.5 text-xs font-semibold text-primary hover:text-primary/95 transition-colors border border-primary/20 bg-primary/5 px-3 py-1.5 rounded-lg cursor-pointer"
                >
                  <span>Open Local Preview Frame</span>
                  <ExternalLink className="h-3.5 w-3.5" />
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* Navigation tabs for Logs vs Metrics vs Timeline */}
        <div className="flex border-b border-border space-x-1 font-semibold text-xs">
          <button
            onClick={() => setActiveTab('logs')}
            className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
              activeTab === 'logs' 
                ? 'border-primary text-primary bg-primary/5' 
                : 'border-transparent text-muted-foreground hover:text-white'
            }`}
          >
            <TermIcon className="h-3.5 w-3.5" />
            <span>Console Logs</span>
          </button>
          
          {deployment.status === 'RUNNING' && (
            <button
              onClick={() => setActiveTab('metrics')}
              className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
                activeTab === 'metrics' 
                  ? 'border-primary text-primary bg-primary/5' 
                  : 'border-transparent text-muted-foreground hover:text-white'
              }`}
            >
              <Cpu className="h-3.5 w-3.5" />
              <span>Container Performance</span>
            </button>
          )}

          <button
            onClick={() => setActiveTab('timeline')}
            className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
              activeTab === 'timeline' 
                ? 'border-primary text-primary bg-primary/5' 
                : 'border-transparent text-muted-foreground hover:text-white'
            }`}
          >
            <History className="h-3.5 w-3.5" />
            <span>Events Timeline ({events.length})</span>
          </button>
        </div>

        {/* Tab display mapping */}
        <div className="mt-4">
          
          {/* TAB 1: STREAMING CONSOLE LOGS */}
          {activeTab === 'logs' && (
            <div className="space-y-4">
              <Terminal logs={logs} />
            </div>
          )}

          {/* TAB 2: LIVE METRICS */}
          {activeTab === 'metrics' && deployment.status === 'RUNNING' && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              
              <div className="bg-card border border-border p-6 rounded-xl space-y-3.5">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-semibold tracking-wider text-zinc-500 uppercase">CPU Usage</span>
                  <Cpu className="h-4 w-4 text-primary" />
                </div>
                <div className="space-y-1">
                  <div className="text-2xl font-bold font-mono text-white">
                    {metrics ? `${metrics.cpuUsagePercent.toFixed(2)}%` : '0.00%'}
                  </div>
                  <p className="text-[10px] text-zinc-500">Real-time docker stats query</p>
                </div>
              </div>

              <div className="bg-card border border-border p-6 rounded-xl space-y-3.5">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-semibold tracking-wider text-zinc-500 uppercase">Memory Footprint</span>
                  <TrendingUp className="h-4 w-4 text-emerald-400" />
                </div>
                <div className="space-y-1">
                  <div className="text-2xl font-bold font-mono text-white">
                    {metrics ? `${metrics.memoryUsageMb.toFixed(1)} MB` : '0.0 MB'}
                  </div>
                  <p className="text-[10px] text-zinc-500">Shared engine allocation limits</p>
                </div>
              </div>

              <div className="bg-card border border-border p-6 rounded-xl space-y-3.5">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-semibold tracking-wider text-zinc-500 uppercase">Container Uptime</span>
                  <Activity className="h-4 w-4 text-indigo-400" />
                </div>
                <div className="space-y-1">
                  <div className="text-2xl font-bold font-mono text-white">
                    {metrics ? formatUptime(metrics.uptimeSeconds) : '0s'}
                  </div>
                  <p className="text-[10px] text-zinc-500">Live duration since docker execution</p>
                </div>
              </div>

            </div>
          )}

          {/* TAB 3: TIMELINE REPLAY */}
          {activeTab === 'timeline' && (
            <div className="bg-card border border-border p-6 rounded-xl space-y-6">
              <h3 className="text-xs font-bold text-zinc-400 tracking-wider uppercase">Pipeline Milestones</h3>
              
              <div className="relative border-l border-zinc-800 ml-3.5 space-y-6">
                {events.length === 0 ? (
                  <div className="pl-6 text-xs text-muted-foreground">
                    No milestone logs registered for this run yet.
                  </div>
                ) : (
                  events.map((e, index) => (
                    <div key={index} className="relative pl-6">
                      
                      {/* Left Dot */}
                      <span className={`absolute -left-1.5 top-1.5 h-3 w-3 rounded-full border ${
                        e.eventType === 'DEPLOYMENT_FAILED'
                          ? 'bg-rose-950 border-rose-500'
                          : e.eventType === 'DEPLOYMENT_READY'
                          ? 'bg-emerald-950 border-emerald-500'
                          : 'bg-zinc-950 border-primary'
                      }`} />

                      <div className="space-y-1 font-mono">
                        <div className="flex items-center space-x-2 text-xs font-bold">
                          <span className={`${
                            e.eventType === 'DEPLOYMENT_FAILED'
                              ? 'text-rose-400'
                              : e.eventType === 'DEPLOYMENT_READY'
                              ? 'text-emerald-400'
                              : 'text-white'
                          }`}>{e.eventType}</span>
                          <span className="text-[10px] text-zinc-600">
                            {new Date(e.createdAt).toLocaleTimeString()}
                          </span>
                        </div>
                        <p className="text-[11px] text-zinc-500 leading-relaxed">{e.message}</p>
                      </div>

                    </div>
                  ))
                )}
              </div>
            </div>
          )}

        </div>
      </div>
    </Layout>
  );
};
