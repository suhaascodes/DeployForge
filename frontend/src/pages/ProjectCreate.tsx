import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { 
  FolderKanban, 
  GitBranch, 
  ArrowLeft, 
  Loader2, 
  HelpCircle,
  Sparkles
} from 'lucide-react';

export const ProjectCreate: React.FC = () => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [repositoryUrl, setRepositoryUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const res = await apiClient.post('/projects', { name, description, repositoryUrl });
      if (res.data.success) {
        navigate(`/project/${res.data.data.id}`);
      }
    } catch (err: any) {
      setError(
        err.response?.data?.message || 
        err.response?.data?.error || 
        'Failed to validate and create project.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Back navigation */}
        <Link 
          to="/" 
          className="inline-flex items-center space-x-1.5 text-xs text-muted-foreground hover:text-white transition-colors cursor-pointer"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Dashboard</span>
        </Link>

        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center space-x-2">
            <FolderKanban className="h-6 w-6 text-primary" />
            <span>Create a New Project</span>
          </h1>
          <p className="text-xs text-muted-foreground mt-1">
            Connect a Git repository. DeployForge will validate repository accessibility E2E.
          </p>
        </div>

        {/* Form Container */}
        <div className="bg-card border border-border p-6 rounded-xl shadow-sm">
          {error && (
            <div className="mb-5 p-3 text-xs bg-rose-950/20 border border-rose-900/30 text-rose-400 rounded-lg whitespace-pre-line">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
                Project Name
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="my-awesome-app"
                className="block w-full px-3 py-2 bg-[#09090b] border border-border rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
              />
              <p className="text-[10px] text-zinc-500 mt-1">
                A unique slug name for your deployment routing.
              </p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
                Git Repository URL
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500 pointer-events-none">
                  <GitBranch className="h-4 w-4" />
                </span>
                <input
                  type="text"
                  required
                  value={repositoryUrl}
                  onChange={(e) => setRepositoryUrl(e.target.value)}
                  placeholder="https://github.com/owner/repository"
                  className="block w-full pl-10 pr-3 py-2 bg-[#09090b] border border-border rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
                />
              </div>
              <div className="mt-2 p-2 bg-zinc-950/40 border border-zinc-900 rounded-lg text-[10px] text-zinc-500 flex items-start space-x-1.5">
                <HelpCircle className="h-3.5 w-3.5 text-zinc-600 shrink-0 mt-0.5" />
                <div className="space-y-0.5 leading-normal">
                  <p>Accepts HTTPS URLs (e.g., <span className="font-mono text-zinc-400">https://github.com/octocat/Spoon-Knife</span>).</p>
                  <p>Public repositories will be pinged instantly using JGit remote refs checks.</p>
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
                Description (Optional)
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="A description explaining what this project builds..."
                rows={3}
                className="block w-full px-3 py-2 bg-[#09090b] border border-border rounded-lg text-sm text-white placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center py-2 px-4 border border-transparent rounded-lg text-sm font-semibold text-white bg-primary hover:bg-primary/95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
                  <span>Validating & Reachability Checking Remote Repository...</span>
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-1.5" />
                  <span>Create & Connect Repository</span>
                </>
              )}
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
};
