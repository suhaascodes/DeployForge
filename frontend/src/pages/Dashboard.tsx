import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { Badge } from '../components/Badge';
import { 
  FolderKanban, 
  GitBranch, 
  Plus, 
  Terminal, 
  ExternalLink, 
  Calendar, 
  Clock,
  ArrowRight,
  GitFork
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
  projectName: string;
  status: string;
  deploymentUrl: string;
  startedAt: string;
  completedAt: string;
  gitCommitHash: string;
  versionTag: string;
}

export const Dashboard: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [projRes, depRes] = await Promise.all([
          apiClient.get('/projects'),
          apiClient.get('/deployments/recent')
        ]);
        if (projRes.data.success) setProjects(projRes.data.data);
        if (depRes.data.success) setDeployments(depRes.data.data);
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const getRelativeTime = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.round(diffMs / 60000);
    const diffHr = Math.round(diffMin / 60);
    const diffDays = Math.round(diffHr / 24);

    if (diffMin < 1) return 'just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHr < 24) return `${diffHr}h ago`;
    return `${diffDays}d ago`;
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

  return (
    <Layout>
      <div className="space-y-10">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-white font-sans">
              Welcome back
            </h1>
            <p className="text-xs text-muted-foreground mt-1">
              Here is an overview of your active deployments and code repositories.
            </p>
          </div>
          <Link
            to="/project/create"
            className="inline-flex items-center space-x-1.5 bg-primary hover:bg-primary/95 text-white font-semibold text-xs px-4 py-2.5 rounded-lg transition-all shadow cursor-pointer self-start sm:self-auto"
          >
            <Plus className="h-4 w-4" />
            <span>Create Project</span>
          </Link>
        </div>

        {/* Projects Cards Grid */}
        <div>
          <div className="flex items-center justify-between mb-4 border-b border-border pb-3">
            <h2 className="text-base font-semibold text-white flex items-center space-x-1.5">
              <FolderKanban className="h-4 w-4 text-zinc-500" />
              <span>Active Projects</span>
            </h2>
            <Link to="/projects" className="text-xs text-primary hover:text-primary/95 flex items-center space-x-1 transition-colors">
              <span>View all</span>
              <ArrowRight className="h-3 w-3" />
            </Link>
          </div>

          {projects.length === 0 ? (
            <div className="border border-dashed border-border p-12 text-center rounded-xl bg-card/20">
              <FolderKanban className="h-10 w-10 text-zinc-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-white">No projects found</p>
              <p className="text-xs text-muted-foreground mt-1 mb-5">
                Create a project to connect your repository and run deployments.
              </p>
              <Link
                to="/project/create"
                className="inline-flex items-center space-x-1.5 bg-zinc-800 hover:bg-zinc-700/80 text-white font-semibold text-xs px-4 py-2 border border-zinc-700 rounded-lg transition-all cursor-pointer"
              >
                <Plus className="h-4 w-4" />
                <span>New Project</span>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {projects.slice(0, 6).map((project) => (
                <Link
                  key={project.id}
                  to={`/project/${project.id}`}
                  className="group block bg-card hover:bg-[#101013] border border-border hover:border-zinc-700 p-5 rounded-xl transition-all shadow-sm"
                >
                  <div className="flex items-start justify-between">
                    <h3 className="text-sm font-bold text-white group-hover:text-primary transition-colors">
                      {project.name}
                    </h3>
                    <GitBranch className="h-4 w-4 text-zinc-600" />
                  </div>
                  <p className="text-xs text-muted-foreground mt-2 line-clamp-2 h-8 leading-relaxed">
                    {project.description || 'No description provided.'}
                  </p>
                  
                  <div className="mt-5 pt-4 border-t border-border/60 flex items-center justify-between text-[11px] text-zinc-500">
                    <div className="flex items-center space-x-1 font-mono max-w-[170px] truncate">
                      <GitFork className="h-3 w-3 text-zinc-600 shrink-0" />
                      <span className="truncate">{project.repositoryUrl.replace(/^https?:\/\/(www\.)?github\.com\//, '')}</span>
                    </div>
                    <div className="flex items-center space-x-1 shrink-0">
                      <Calendar className="h-3 w-3 text-zinc-600" />
                      <span>{formatDate(project.createdAt)}</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Recent Deployments Table */}
        <div>
          <div className="flex items-center justify-between mb-4 border-b border-border pb-3">
            <h2 className="text-base font-semibold text-white flex items-center space-x-1.5">
              <Terminal className="h-4 w-4 text-zinc-500" />
              <span>Recent Deployments</span>
            </h2>
          </div>

          {deployments.length === 0 ? (
            <div className="border border-dashed border-border p-8 text-center rounded-xl bg-card/20 text-xs text-muted-foreground">
              No deployments triggered yet. Create a project to run deployments.
            </div>
          ) : (
            <div className="border border-border rounded-xl bg-card overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-zinc-950/60 border-b border-border text-zinc-500 font-semibold select-none">
                      <th className="p-4">Project</th>
                      <th className="p-4">Deployment ID</th>
                      <th className="p-4">Status</th>
                      <th className="p-4">Commit / Version</th>
                      <th className="p-4">Triggered</th>
                      <th className="p-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60 text-zinc-300">
                    {deployments.map((dep) => (
                      <tr key={dep.id} className="hover:bg-zinc-950/30 transition-colors">
                        <td className="p-4 font-semibold text-white">
                          <Link to={`/project/${dep.projectId}`} className="hover:text-primary transition-colors">
                            {dep.projectName}
                          </Link>
                        </td>
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
                          <span className="inline-flex items-center space-x-1">
                            <Clock className="h-3 w-3 shrink-0" />
                            <span>{getRelativeTime(dep.startedAt)}</span>
                          </span>
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
