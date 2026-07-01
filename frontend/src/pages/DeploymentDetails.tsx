import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { Badge } from '../components/Badge';
import { Terminal } from '../components/Terminal';
import { 
  ArrowLeft, 
  Clock, 
  ExternalLink, 
  GitCommit, 
  Loader2, 
  Server,
  CheckCircle2,
  XCircle,
  Circle,
  Terminal as TermIcon
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
  
  // V2 Container Info
  containerId?: string;
  containerName?: string;
  imageTag?: string;
  hostPort?: number;
  buildDurationMs?: number;
  frameworkDetected?: string;
}

interface LogLine {
  id: string;
  deploymentId: string;
  message: string;
  level: string;
  timestamp: string;
}

export const DeploymentDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [deployment, setDeployment] = useState<Deployment | null>(null);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchLogsAndStatus = async () => {
    try {
      const [depRes, logRes] = await Promise.all([
        apiClient.get(`/deployments/${id}`),
        apiClient.get(`/logs/${id}`)
      ]);
      if (depRes.data.success) setDeployment(depRes.data.data);
      if (logRes.data.success) setLogs(logRes.data.data);
    } catch (err) {
      console.error('Failed to update deployment logs/status', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogsAndStatus();

    // Poll every 2 seconds if status is in active/non-terminal states
    const interval = setInterval(() => {
      if (deployment && (
        deployment.status === 'QUEUED' || 
        deployment.status === 'CLONING' || 
        deployment.status === 'BUILDING' || 
        deployment.status === 'CREATING_IMAGE' || 
        deployment.status === 'STARTING_CONTAINER'
      )) {
        fetchLogsAndStatus();
      }
    }, 2000);

    return () => clearInterval(interval);
  }, [id, deployment?.status]);

  const getDuration = (start: string, end: string) => {
    if (!start) return '-';
    const endTime = end ? new Date(end).getTime() : new Date().getTime();
    const diff = endTime - new Date(start).getTime();
    const sec = Math.round(diff / 1000);
    if (sec < 60) return `${sec}s`;
    return `${Math.round(sec / 60)}m ${sec % 60}s`;
  };

  const getTimelineSteps = () => {
    if (!deployment) return [];
    const status = deployment.status;

    return [
      {
        name: 'Cloning',
        desc: 'Clones git repository source files via JGit',
        state: status === 'CLONING' ? 'active' :
               (status === 'QUEUED') ? 'pending' :
               (status === 'BUILD_FAILED' && !deployment.frameworkDetected) ? 'failed' : 'complete'
      },
      {
        name: 'Building',
        desc: 'Executes compiler (npm run build / mvn package)',
        state: status === 'BUILDING' ? 'active' :
               (status === 'QUEUED' || status === 'CLONING') ? 'pending' :
               (status === 'BUILD_FAILED' && deployment.frameworkDetected && !deployment.buildDurationMs) ? 'failed' : 'complete'
      },
      {
        name: 'Creating Image',
        desc: 'Generates dynamic Dockerfile & runs docker build',
        state: status === 'CREATING_IMAGE' ? 'active' :
               (status === 'QUEUED' || status === 'CLONING' || status === 'BUILDING') ? 'pending' :
               (status === 'BUILD_FAILED' && deployment.buildDurationMs) ? 'failed' : 'complete'
      },
      {
        name: 'Starting Container',
        desc: 'Launches container runtime and checks connection health',
        state: status === 'STARTING_CONTAINER' ? 'active' :
               (status === 'RUNNING') ? 'complete' :
               (status === 'RUNTIME_FAILED') ? 'failed' : 'pending'
      }
    ];
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
          <XCircle className="h-10 w-10 text-rose-500 mx-auto mb-3" />
          <h2 className="text-lg font-bold text-white">Deployment Not Found</h2>
          <p className="text-xs text-muted-foreground mt-1 mb-5">
            This deployment run does not exist or you do not have permission to view it.
          </p>
          <Link to="/" className="text-primary hover:underline text-xs">Return to Dashboard</Link>
        </div>
      </Layout>
    );
  }

  const steps = getTimelineSteps();

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
              
              {/* Action / Trigger indicator */}
              {(deployment.status === 'QUEUED' || deployment.status === 'CLONING' || deployment.status === 'BUILDING' || deployment.status === 'CREATING_IMAGE' || deployment.status === 'STARTING_CONTAINER') && (
                <div className="flex items-center space-x-2 text-zinc-400 text-xs font-medium">
                  <Loader2 className="h-4 w-4 animate-spin text-primary" />
                  <span>Deployment in progress...</span>
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
              {deployment.gitCommitHash && (
                <span className="flex items-center space-x-1">
                  <GitCommit className="h-3.5 w-3.5 text-zinc-600" />
                  <span>Commit: {deployment.gitCommitHash.substring(0, 7)} ({deployment.versionTag})</span>
                </span>
              )}
              {deployment.buildDurationMs && (
                <span className="flex items-center space-x-1">
                  <TermIcon className="h-3.5 w-3.5 text-zinc-600" />
                  <span>Compiled in: {Math.round(deployment.buildDurationMs / 1000)}s</span>
                </span>
              )}
              {deployment.frameworkDetected && (
                <span className="flex items-center space-x-1">
                  <Badge status={deployment.frameworkDetected} />
                </span>
              )}
            </div>

            {/* Container Details Section */}
            {deployment.containerId && (
              <div className="border-t border-border/60 pt-4 mt-2 grid grid-cols-2 md:grid-cols-4 gap-4 text-[10px] text-zinc-400 font-mono">
                <div>
                  <span className="text-zinc-500">Container ID:</span> <span className="text-zinc-300 font-semibold">{deployment.containerId.substring(0, 12)}</span>
                </div>
                <div>
                  <span className="text-zinc-500">Container Name:</span> <span className="text-zinc-300 font-semibold truncate block md:inline max-w-[120px]">{deployment.containerName}</span>
                </div>
                <div>
                  <span className="text-zinc-500">Image Tag:</span> <span className="text-zinc-300 font-semibold">{deployment.imageTag}</span>
                </div>
                <div>
                  <span className="text-zinc-500">Host Port:</span> <span className="text-zinc-300 font-semibold">{deployment.hostPort}</span>
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
                  to={`/preview/${deployment.id}`}
                  className="inline-flex items-center space-x-1.5 text-xs font-semibold text-emerald-400 hover:text-emerald-300 transition-colors border border-emerald-900/40 bg-emerald-950/20 px-3 py-1.5 rounded-lg"
                >
                  <span>Open Local Preview Frame</span>
                  <ExternalLink className="h-3.5 w-3.5" />
                </Link>
                <span className="text-[10px] text-zinc-500 hidden sm:inline select-none">
                  (Simulated container iframe preview)
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Steps Timeline Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 bg-card border border-border p-6 rounded-xl shadow-sm">
          {steps.map((step, idx) => {
            let Icon = Circle;
            let iconColor = 'text-zinc-600';
            let bgClass = 'bg-zinc-950';

            if (step.state === 'complete') {
              Icon = CheckCircle2;
              iconColor = 'text-emerald-500';
              bgClass = 'bg-emerald-950/20';
            } else if (step.state === 'active') {
              Icon = Loader2;
              iconColor = 'text-primary animate-spin';
              bgClass = 'bg-primary/5';
            } else if (step.state === 'failed') {
              Icon = XCircle;
              iconColor = 'text-rose-500';
              bgClass = 'bg-rose-950/20';
            }

            return (
              <div key={idx} className={`p-4 rounded-xl border border-border/80 flex items-start space-x-3 ${bgClass}`}>
                <Icon className={`h-5 w-5 shrink-0 mt-0.5 ${iconColor}`} />
                <div className="space-y-0.5">
                  <h3 className="text-xs font-bold text-white">{step.name}</h3>
                  <p className="text-[10px] text-muted-foreground leading-normal">{step.desc}</p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Terminal logs display */}
        <div className="space-y-3">
          <h2 className="text-sm font-semibold text-white">Execution Console Logs</h2>
          <Terminal logs={logs} />
        </div>
      </div>
    </Layout>
  );
};
