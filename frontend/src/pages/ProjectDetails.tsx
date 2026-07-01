import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { Badge } from '../components/Badge';
import { 
  GitBranch, 
  ArrowLeft, 
  Play, 
  Trash2, 
  ExternalLink, 
  Clock, 
  Calendar,
  Layers,
  AlertTriangle,
  Key,
  Webhook,
  RefreshCw,
  Plus,
  Lock,
  GitCommit,
  User,
  Info
} from 'lucide-react';

interface Project {
  id: string;
  name: string;
  description: string;
  repositoryUrl: string;
  createdAt: string;
  webhookSecret: string;
}

interface Deployment {
  id: string;
  projectId: string;
  status: string;
  deploymentUrl: string;
  startedAt: string;
  completedAt: string;
  gitCommitHash: string;
  versionTag: string;
  triggerType: string;
  githubCommitHash: string;
  githubCommitMessage: string;
  githubAuthor: string;
  sourceDeploymentId?: string;
}

interface EnvVar {
  id: string;
  key: string;
  value: string; // Masked
  createdAt: string;
  updatedAt: string;
}

export const ProjectDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [envVars, setEnvVars] = useState<EnvVar[]>([]);
  
  const [activeTab, setActiveTab] = useState<'deployments' | 'env-vars' | 'webhook'>('deployments');
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // New Env Var State
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');
  const [savingEnv, setSavingEnv] = useState(false);

  const fetchEnvVars = async () => {
    try {
      const res = await apiClient.get(`/projects/${id}/env-vars`);
      if (res.data.success) {
        setEnvVars(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load environment variables', err);
    }
  };

  const fetchProjectData = async () => {
    try {
      const [projRes, depRes] = await Promise.all([
        apiClient.get(`/projects/${id}`),
        apiClient.get(`/deployments/project/${id}`)
      ]);
      if (projRes.data.success) setProject(projRes.data.data);
      if (depRes.data.success) setDeployments(depRes.data.data);
      await fetchEnvVars();
    } catch (err) {
      console.error('Failed to load project details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjectData();
    // Poll project deployments if any are active
    const interval = setInterval(() => {
      const activeDeps = deployments.some(d => 
        d.status === 'QUEUED' || d.status === 'CLONING' || d.status === 'BUILDING' || d.status === 'CREATING_IMAGE' || d.status === 'STARTING_CONTAINER'
      );
      if (activeDeps) {
        fetchProjectData();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [id, deployments.length]);

  const handleDeploy = async () => {
    setTriggering(true);
    try {
      const res = await apiClient.post('/deployments', { projectId: id });
      if (res.data.success) {
        fetchProjectData();
      }
    } catch (err) {
      alert('Failed to trigger deployment: ' + err);
    } finally {
      setTriggering(false);
    }
  };

  const handleRedeploy = async (depId: string) => {
    if (!window.confirm('Are you sure you want to redeploy this commit version?')) return;
    try {
      const res = await apiClient.post(`/deployments/${depId}/redeploy`);
      if (res.data.success) {
        fetchProjectData();
        setActiveTab('deployments');
      }
    } catch (err) {
      alert('Failed to trigger redeployment: ' + err);
    }
  };

  const handleSaveEnvVar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newKey || !newValue) return;
    setSavingEnv(true);
    try {
      const res = await apiClient.post(`/projects/${id}/env-vars`, {
        key: newKey,
        value: newValue
      });
      if (res.data.success) {
        setNewKey('');
        setNewValue('');
        fetchEnvVars();
      }
    } catch (err: any) {
      alert('Failed to save environment variable: ' + (err.response?.data?.message || err.message));
    } finally {
      setSavingEnv(false);
    }
  };

  const handleDeleteEnvVar = async (varId: string) => {
    if (!window.confirm('Are you sure you want to delete this secret key?')) return;
    try {
      const res = await apiClient.delete(`/projects/${id}/env-vars/${varId}`);
      if (res.data.success) {
        fetchEnvVars();
      }
    } catch (err) {
      alert('Failed to delete environment variable: ' + err);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you absolutely sure you want to delete this project? This will permanently remove all associated deployments and logs.')) {
      return;
    }
    setDeleting(true);
    try {
      const res = await apiClient.delete(`/projects/${id}`);
      if (res.data.success) {
        navigate('/');
      }
    } catch (err) {
      alert('Failed to delete project: ' + err);
      setDeleting(false);
    }
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const getDuration = (start: string, end: string) => {
    if (!start || !end) return '';
    const diff = new Date(end).getTime() - new Date(start).getTime();
    const sec = Math.round(diff / 1000);
    if (sec < 60) return `${sec}s`;
    return `${Math.round(sec / 60)}m ${sec % 60}s`;
  };

  if (loading) {
    return (
      <Layout>
        <div className="h-[60vh] flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      </Layout>
    );
  }

  if (!project) {
    return (
      <Layout>
        <div className="text-center py-12">
          <AlertTriangle className="h-10 w-10 text-amber-500 mx-auto mb-3" />
          <h2 className="text-lg font-bold text-white">Project Not Found</h2>
          <p className="text-xs text-muted-foreground mt-1 mb-5">
            This project does not exist or you do not have permission to view it.
          </p>
          <Link to="/" className="text-primary hover:underline text-xs">Return to Dashboard</Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-8">
        {/* Navigation / Actions Bar */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <Link 
            to="/" 
            className="inline-flex items-center space-x-1.5 text-xs text-muted-foreground hover:text-white transition-colors cursor-pointer"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Dashboard</span>
          </Link>
          
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="flex items-center space-x-1.5 px-3 py-1.5 text-xs text-rose-400 border border-transparent hover:border-rose-900/30 hover:bg-rose-950/20 rounded-md transition-all cursor-pointer disabled:opacity-50"
          >
            <Trash2 className="h-3.5 w-3.5" />
            <span>{deleting ? 'Deleting...' : 'Delete Project'}</span>
          </button>
        </div>

        {/* Project Header Info */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6 bg-card border border-border p-6 rounded-xl shadow-sm">
          <div className="space-y-2">
            <h1 className="text-2xl font-bold tracking-tight text-white">{project.name}</h1>
            <p className="text-xs text-muted-foreground max-w-xl">{project.description || 'No description provided.'}</p>
            <div className="flex flex-wrap gap-4 text-[11px] text-zinc-500 pt-1.5 font-mono">
              <span className="flex items-center space-x-1">
                <GitBranch className="h-3.5 w-3.5 text-zinc-600" />
                <span className="text-zinc-400 select-all">{project.repositoryUrl}</span>
              </span>
              <span className="flex items-center space-x-1">
                <Calendar className="h-3.5 w-3.5 text-zinc-600" />
                <span>Created {formatDate(project.createdAt)}</span>
              </span>
            </div>
          </div>

          <button
            onClick={handleDeploy}
            disabled={triggering}
            className="flex items-center justify-center space-x-1.5 bg-primary hover:bg-primary/95 text-white font-semibold text-xs px-5 py-2.5 rounded-lg shadow transition-all cursor-pointer shrink-0 disabled:opacity-50"
          >
            <Play className="h-3.5 w-3.5 fill-current" />
            <span>{triggering ? 'Queuing build...' : 'Deploy Repository'}</span>
          </button>
        </div>

        {/* V3 Navigation Tabs */}
        <div className="flex border-b border-border space-x-1 font-semibold text-xs">
          <button
            onClick={() => setActiveTab('deployments')}
            className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
              activeTab === 'deployments' 
                ? 'border-primary text-primary bg-primary/5' 
                : 'border-transparent text-muted-foreground hover:text-white'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            <span>Deployments ({deployments.length})</span>
          </button>
          
          <button
            onClick={() => setActiveTab('env-vars')}
            className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
              activeTab === 'env-vars' 
                ? 'border-primary text-primary bg-primary/5' 
                : 'border-transparent text-muted-foreground hover:text-white'
            }`}
          >
            <Key className="h-3.5 w-3.5" />
            <span>Environment Variables ({envVars.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('webhook')}
            className={`flex items-center space-x-2 px-4 py-2 border-b-2 cursor-pointer transition-all ${
              activeTab === 'webhook' 
                ? 'border-primary text-primary bg-primary/5' 
                : 'border-transparent text-muted-foreground hover:text-white'
            }`}
          >
            <Webhook className="h-3.5 w-3.5" />
            <span>Git Integration</span>
          </button>
        </div>

        {/* Tab contents */}
        <div className="mt-4">
          
          {/* TAB 1: DEPLOYMENTS HISTORY */}
          {activeTab === 'deployments' && (
            <div className="space-y-4">
              {deployments.length === 0 ? (
                <div className="border border-dashed border-border p-12 text-center rounded-xl bg-card/20">
                  <Clock className="h-8 w-8 text-zinc-600 mx-auto mb-3" />
                  <p className="text-sm font-semibold text-white">No deployments yet</p>
                  <p className="text-xs text-muted-foreground mt-1 mb-5">
                    Trigger your first deployment to build and run the application.
                  </p>
                  <button
                    onClick={handleDeploy}
                    className="inline-flex items-center space-x-1.5 bg-zinc-800 hover:bg-zinc-700/80 text-white font-semibold text-xs px-4 py-2 border border-zinc-700 rounded-lg transition-all cursor-pointer"
                  >
                    <Play className="h-3.5 w-3.5 fill-current" />
                    <span>Deploy Now</span>
                  </button>
                </div>
              ) : (
                <div className="border border-border rounded-xl bg-card overflow-hidden shadow-sm">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="bg-zinc-950/60 border-b border-border text-zinc-500 font-semibold select-none">
                          <th className="p-4">Deployment ID</th>
                          <th className="p-4">Trigger / Source</th>
                          <th className="p-4">Status</th>
                          <th className="p-4">Commit Info</th>
                          <th className="p-4">Date</th>
                          <th className="p-4">Duration</th>
                          <th className="p-4 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border/60 text-zinc-300">
                        {deployments.map((dep) => (
                          <tr key={dep.id} className="hover:bg-zinc-950/30 transition-colors">
                            <td className="p-4 font-mono text-zinc-400">
                              {dep.id.substring(0, 8)}
                              {dep.sourceDeploymentId && (
                                <span className="block text-[10px] text-zinc-600">
                                  redeploy from {dep.sourceDeploymentId.substring(0, 8)}
                                </span>
                              )}
                            </td>
                            <td className="p-4">
                              <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold border ${
                                dep.triggerType === 'GITHUB_WEBHOOK'
                                  ? 'bg-zinc-900 border-zinc-800 text-zinc-400'
                                  : 'bg-primary/5 border-primary/20 text-primary'
                              }`}>
                                {dep.triggerType === 'GITHUB_WEBHOOK' ? 'GitHub Push' : 'Manual'}
                              </span>
                            </td>
                            <td className="p-4">
                              <Badge status={dep.status} />
                            </td>
                            <td className="p-4">
                              <div className="space-y-1 max-w-xs">
                                <div className="flex items-center space-x-1.5 text-zinc-400">
                                  <GitCommit className="h-3.5 w-3.5 shrink-0 text-zinc-500" />
                                  <span className="font-mono bg-zinc-900 px-1.5 py-0.5 rounded border border-zinc-800 text-[11px] truncate">
                                    {dep.gitCommitHash ? dep.gitCommitHash.substring(0, 7) : 'HEAD'}
                                  </span>
                                  {dep.githubAuthor && (
                                    <span className="flex items-center space-x-0.5 text-[10px] text-zinc-500 truncate">
                                      <User className="h-3 w-3" />
                                      <span>{dep.githubAuthor}</span>
                                    </span>
                                  )}
                                </div>
                                {dep.githubCommitMessage && (
                                  <p className="text-[11px] text-zinc-500 truncate select-all">{dep.githubCommitMessage}</p>
                                )}
                              </div>
                            </td>
                            <td className="p-4 text-zinc-500">
                              {new Date(dep.startedAt).toLocaleString()}
                            </td>
                            <td className="p-4 text-zinc-500 font-mono">
                              {getDuration(dep.startedAt, dep.completedAt) || '-'}
                            </td>
                            <td className="p-4 text-right">
                              <div className="flex items-center justify-end space-x-2">
                                <button
                                  onClick={() => handleRedeploy(dep.id)}
                                  title="Redeploy this commit version"
                                  className="inline-flex items-center justify-center p-1.5 text-zinc-500 hover:text-white border border-zinc-800 hover:border-zinc-700 bg-zinc-900 rounded-md transition-colors cursor-pointer"
                                >
                                  <RefreshCw className="h-3.5 w-3.5" />
                                </button>
                                <Link
                                  to={`/deployment/${dep.id}`}
                                  className="inline-flex items-center space-x-1 text-primary hover:text-primary/90 font-medium transition-colors cursor-pointer"
                                >
                                  <span>Inspect</span>
                                  <ExternalLink className="h-3.5 w-3.5" />
                                </Link>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 2: ENVIRONMENT VARIABLES */}
          {activeTab === 'env-vars' && (
            <div className="space-y-6">
              <div className="bg-card border border-border p-6 rounded-xl shadow-sm space-y-4">
                <div className="flex items-start space-x-3">
                  <Info className="h-5 w-5 text-primary shrink-0 mt-0.5" />
                  <div className="space-y-1">
                    <h3 className="text-sm font-semibold text-white">Environment Secrets Configurations</h3>
                    <p className="text-xs text-muted-foreground leading-relaxed">
                      Keys defined below are encrypted using industry-standard AES-256-GCM. Values are masked and never exposed to raw API responses. 
                      Once saved, variables will be injected automatically via <code className="bg-zinc-900 px-1 py-0.5 rounded text-[11px] text-zinc-300">-e KEY=VAL</code> parameters upon container launch.
                    </p>
                  </div>
                </div>

                <form onSubmit={handleSaveEnvVar} className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-semibold text-zinc-400 tracking-wider uppercase">Key Name</label>
                    <input
                      type="text"
                      placeholder="e.g. DATABASE_URL"
                      value={newKey}
                      onChange={(e) => setNewKey(e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, ''))}
                      className="w-full bg-zinc-950 border border-border focus:border-primary/50 text-white rounded-lg px-3 py-2 text-xs focus:outline-none transition-all placeholder:text-zinc-700 font-mono"
                      required
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-semibold text-zinc-400 tracking-wider uppercase">Value Secret</label>
                    <input
                      type="password"
                      placeholder="••••••••••••••••"
                      value={newValue}
                      onChange={(e) => setNewValue(e.target.value)}
                      className="w-full bg-zinc-950 border border-border focus:border-primary/50 text-white rounded-lg px-3 py-2 text-xs focus:outline-none transition-all placeholder:text-zinc-700 font-mono"
                      required
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="submit"
                      disabled={savingEnv}
                      className="w-full flex items-center justify-center space-x-1.5 bg-primary hover:bg-primary/95 text-white font-semibold text-xs py-2.5 rounded-lg transition-all cursor-pointer disabled:opacity-50"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>{savingEnv ? 'Saving...' : 'Add Variable'}</span>
                    </button>
                  </div>
                </form>
              </div>

              {/* List of active variables */}
              <div className="border border-border rounded-xl bg-card overflow-hidden">
                <div className="p-4 border-b border-border bg-zinc-950/40 flex items-center justify-between">
                  <span className="text-xs font-semibold text-white flex items-center space-x-1.5">
                    <Lock className="h-3.5 w-3.5 text-zinc-500" />
                    <span>Active secrets ({envVars.length})</span>
                  </span>
                </div>

                {envVars.length === 0 ? (
                  <div className="p-8 text-center text-xs text-muted-foreground">
                    No variables configured for this project.
                  </div>
                ) : (
                  <div className="divide-y divide-border/60">
                    {envVars.map((v) => (
                      <div key={v.id} className="p-4 flex items-center justify-between hover:bg-zinc-950/10 transition-colors">
                        <div className="space-y-1 font-mono">
                          <div className="text-xs font-semibold text-white">{v.key}</div>
                          <div className="text-[11px] text-zinc-600">{v.value}</div>
                        </div>
                        <button
                          onClick={() => handleDeleteEnvVar(v.id)}
                          className="p-1.5 border border-transparent hover:border-zinc-800 hover:bg-zinc-900 rounded-md text-zinc-500 hover:text-rose-400 transition-colors cursor-pointer"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 3: WEBHOOKS */}
          {activeTab === 'webhook' && (
            <div className="bg-card border border-border p-6 rounded-xl shadow-sm space-y-6">
              <div className="space-y-2">
                <h3 className="text-sm font-semibold text-white flex items-center space-x-1.5">
                  <Webhook className="h-4 w-4 text-primary" />
                  <span>GitHub Push Auto-Deployment Webhook</span>
                </h3>
                <p className="text-xs text-muted-foreground leading-relaxed max-w-2xl">
                  Configure this webhook URL inside your GitHub repository settings. Whenever you push new commits to GitHub, 
                  DeployForge will automatically verify the signature payload, pull down the changes, compile the project, and redeploy it.
                </p>
              </div>

              <div className="space-y-4 pt-2">
                <div className="space-y-1.5">
                  <label className="text-[11px] font-semibold text-zinc-400 tracking-wider uppercase">GitHub Webhook Payload URL</label>
                  <div className="flex">
                    <input
                      type="text"
                      readOnly
                      value={`${window.location.origin.replace('3000', '8080')}/api/webhooks/github/${project.id}`}
                      className="w-full md:max-w-xl bg-zinc-950 border border-border text-zinc-300 rounded-lg px-3 py-2 text-xs focus:outline-none font-mono select-all cursor-text"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[11px] font-semibold text-zinc-400 tracking-wider uppercase">Webhook Secret Key</label>
                  <div className="flex">
                    <input
                      type="text"
                      readOnly
                      value={project.webhookSecret || ''}
                      className="w-full md:max-w-xl bg-zinc-950 border border-border text-zinc-300 rounded-lg px-3 py-2 text-xs focus:outline-none font-mono select-all cursor-text"
                    />
                  </div>
                  <p className="text-[10px] text-zinc-500">
                    Set Content type to <code className="bg-zinc-950 px-1 py-0.5 rounded text-zinc-400">application/json</code> and copy this secret key as the Webhook Secret key in GitHub.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};
