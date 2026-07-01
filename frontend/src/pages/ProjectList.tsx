import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { 
  FolderKanban, 
  Plus, 
  GitBranch, 
  Search, 
  Calendar,
  ChevronRight
} from 'lucide-react';

interface Project {
  id: string;
  name: string;
  description: string;
  repositoryUrl: string;
  createdAt: string;
}

export const ProjectList: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const res = await apiClient.get('/projects');
        if (res.data.success) {
          setProjects(res.data.data);
        }
      } catch (err) {
        console.error('Failed to load projects list', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProjects();
  }, []);

  const filteredProjects = projects.filter(p => 
    p.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    p.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.repositoryUrl.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
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
      <div className="space-y-6">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center space-x-2">
              <FolderKanban className="h-6 w-6 text-primary" />
              <span>Projects</span>
            </h1>
            <p className="text-xs text-muted-foreground mt-1">
              Manage your connected code repositories and deployment routing.
            </p>
          </div>
          <Link
            to="/project/create"
            className="inline-flex items-center space-x-1.5 bg-primary hover:bg-primary/95 text-white font-semibold text-xs px-4 py-2 rounded-lg shadow transition-all cursor-pointer self-start sm:self-auto"
          >
            <Plus className="h-4 w-4" />
            <span>Create Project</span>
          </Link>
        </div>

        {/* Search Bar */}
        <div className="relative max-w-md">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500 pointer-events-none">
            <Search className="h-4 w-4" />
          </span>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search projects by name, description, or URL..."
            className="block w-full pl-10 pr-3 py-2 bg-card border border-border rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
          />
        </div>

        {/* Projects List Grid */}
        {filteredProjects.length === 0 ? (
          <div className="border border-dashed border-border p-12 text-center rounded-xl bg-card/20">
            <FolderKanban className="h-10 w-10 text-zinc-600 mx-auto mb-3" />
            <p className="text-sm font-semibold text-white">No projects found</p>
            <p className="text-xs text-muted-foreground mt-1 mb-5">
              {searchTerm ? 'No projects match your search query.' : 'Create a project to connect your repository.'}
            </p>
            {!searchTerm && (
              <Link
                to="/project/create"
                className="inline-flex items-center space-x-1.5 bg-zinc-800 hover:bg-zinc-700/80 text-white font-semibold text-xs px-4 py-2 border border-zinc-700 rounded-lg transition-all cursor-pointer"
              >
                <Plus className="h-4 w-4" />
                <span>New Project</span>
              </Link>
            )}
          </div>
        ) : (
          <div className="border border-border rounded-xl bg-card divide-y divide-border/60 overflow-hidden shadow-sm">
            {filteredProjects.map((project) => (
              <Link
                key={project.id}
                to={`/project/${project.id}`}
                className="group flex flex-col sm:flex-row sm:items-center sm:justify-between p-5 hover:bg-zinc-950/30 transition-colors gap-4"
              >
                <div className="space-y-1">
                  <h3 className="text-sm font-bold text-white group-hover:text-primary transition-colors">
                    {project.name}
                  </h3>
                  <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed max-w-xl">
                    {project.description || 'No description provided.'}
                  </p>
                  <div className="flex items-center space-x-1.5 text-[10px] text-zinc-500 font-mono pt-1">
                    <GitBranch className="h-3.5 w-3.5 text-zinc-600" />
                    <span>{project.repositoryUrl}</span>
                  </div>
                </div>

                <div className="flex items-center space-x-6 shrink-0 justify-between sm:justify-end">
                  <div className="flex items-center space-x-1 text-[11px] text-zinc-500">
                    <Calendar className="h-3.5 w-3.5 text-zinc-600" />
                    <span>Created {formatDate(project.createdAt)}</span>
                  </div>
                  <ChevronRight className="h-4 w-4 text-zinc-600 group-hover:text-white group-hover:translate-x-0.5 transition-all hidden sm:block" />
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};
