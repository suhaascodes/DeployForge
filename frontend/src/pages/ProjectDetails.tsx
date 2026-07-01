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
  AlertTriangle
} from 'lucide-react';

interface Project {
  id: string;
  name: string;
  description: string;
  repositoryUrl: string;
  createdAt: string;
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
}

export const ProjectDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchProjectData = async () => {
    try {
      const [projRes, depRes] = await Promise.all([
        apiClient.get(`/projects/${id}`),
        apiClient.get(`/deployments/project/${id}`)
      ]);
      if (projRes.data.success) setProject(projRes.data.data);
      if (depRes.data.success) setDeployments(depRes.data.data);
    } catch (err) {
      console.error('Failed to load project details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjectData();
    // Poll project deployments if any are in queued/building/deploying states
    const interval = setInterval(() => {
      const activeDeps = deployments.some(d => 
        d.status === 'QUEUED' || d.status === 'BUILDING' || d.status === 'DEPLOYING'
      );
      if (activeDeps) {
        fetchProjectData();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [id, deployments]);

  const handleDeploy = async () => {
    setTriggering(true);
    try {
      const res = await apiClient.post('/deployments', { projectId: id });
      if (res.data.success) {
        // Fetch new deployment list immediately
        fetchProjectData();
      }
    } catch (err) {
      alert('Failed to trigger deployment: ' + err);
    } finally {
      setTriggering(false);
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

        {/* Deployments History List */}
        <div className="space-y-4">
          <h2 className="text-base font-semibold text-white flex items-center space-x-1.5 border-b border-border pb-3">
            <Layers className="h-4 w-4 text-zinc-500" />
            <span>Deployment History</span>
          </h2>

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
                      <th className="p-4">Status</th>
                      <th className="p-4">Commit / Version</th>
                      <th className="p-4">Started At</th>
                      <th className="p-4">Duration</th>
                      <th className="p-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60 text-zinc-300">
                    {deployments.map((dep) => (
                      <tr key={dep.id} className="hover:bg-zinc-950/30 transition-colors">
                        <td className="p-4 font-mono text-zinc-500">
                          {dep.id.substring(0, 8)}
                        </td>
                        <td className="p-4">
                          <Badge status={dep.status} />
                        </td>
                        <td className="p-4 font-mono text-zinc-500">
                          {dep.gitCommitHash ? (
                            <span className="bg-zinc-900 px-1.5 py-0.5 rounded border border-zinc-800">
                              {dep.gitCommitHash.substring(0, 7)}
                            </span>
                          ) : '-'}
                          {dep.versionTag && (
                            <span className="ml-1 text-[10px] text-zinc-600">
                              ({dep.versionTag})
                            </span>
                          )}
                        </td>
                        <td className="p-4 text-zinc-500">
                          {new Date(dep.startedAt).toLocaleString()}
                        </td>
                        <td className="p-4 text-zinc-500 font-mono">
                          {getDuration(dep.startedAt, dep.completedAt) || '-'}
                        </td>
                        <td className="p-4 text-right">
                          <Link
                            to={`/deployment/${dep.id}`}
                            className="inline-flex items-center space-x-1 text-primary hover:text-primary/90 font-medium transition-colors cursor-pointer"
                          >
                            <span>Inspect</span>
                            <ExternalLink className="h-3.5 w-3.5" />
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};
