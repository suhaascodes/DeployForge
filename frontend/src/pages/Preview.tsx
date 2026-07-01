import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { Layout } from '../components/Layout';
import { 
  ArrowLeft, 
  Globe, 
  Lock, 
  RotateCw, 
  Activity
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
  hostPort?: number;
}

export const Preview: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [deployment, setDeployment] = useState<Deployment | null>(null);
  const [loading, setLoading] = useState(true);
  const [key, setKey] = useState(0); // Key used to force iframe reload

  useEffect(() => {
    const fetchDeployment = async () => {
      try {
        const res = await apiClient.get(`/deployments/${id}`);
        if (res.data.success) {
          setDeployment(res.data.data);
        }
      } catch (err) {
        console.error('Failed to load preview details', err);
      } finally {
        setLoading(false);
      }
    };
    fetchDeployment();
  }, [id]);

  const handleReload = () => {
    setKey(prev => prev + 1);
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

  if (!deployment || (deployment.status !== 'RUNNING' && deployment.status !== 'DEPLOYING')) {
    return (
      <Layout>
        <div className="max-w-md mx-auto text-center py-12 space-y-4">
          <div className="inline-flex p-3 rounded-full bg-amber-950/20 border border-amber-900/30 text-amber-500">
            <Activity className="h-8 w-8" />
          </div>
          <h2 className="text-lg font-bold text-white">Container is Booting</h2>
          <p className="text-xs text-muted-foreground">
            The application preview is only available once the container status reaches <strong>RUNNING</strong>.
          </p>
          <Link to={`/deployment/${id}`} className="text-primary hover:underline text-xs inline-block">
            Go back to Logs Timeline
          </Link>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Navigation back */}
        <div className="flex items-center justify-between border-b border-border pb-4">
          <Link 
            to={`/deployment/${id}`} 
            className="inline-flex items-center space-x-1.5 text-xs text-muted-foreground hover:text-white transition-colors cursor-pointer"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Logs</span>
          </Link>
          <div className="text-xs text-zinc-500 font-mono">
            Active Container Live Preview
          </div>
        </div>

        {/* Mock Browser Shell */}
        <div className="border border-zinc-800 rounded-xl overflow-hidden shadow-2xl flex flex-col bg-zinc-950">
          
          {/* Browser Address Bar Header */}
          <div className="bg-zinc-900 px-4 py-2 border-b border-zinc-800 flex items-center space-x-4 shrink-0">
            <div className="flex space-x-1.5 shrink-0 select-none">
              <div className="w-3 h-3 rounded-full bg-rose-500/80" />
              <div className="w-3 h-3 rounded-full bg-amber-500/80" />
              <div className="w-3 h-3 rounded-full bg-emerald-500/80" />
            </div>

            <div className="flex-1 bg-zinc-950 rounded-md border border-zinc-800/80 h-7 px-3 flex items-center justify-between text-xs text-zinc-400 font-mono">
              <div className="flex items-center space-x-1.5 truncate max-w-full">
                <Lock className="h-3.5 w-3.5 text-emerald-500 shrink-0" />
                <span className="text-zinc-600 select-none">http://</span>
                <span className="text-zinc-300 truncate select-all">{deployment.deploymentUrl.replace('http://', '')}</span>
              </div>
              <RotateCw 
                onClick={handleReload}
                className="h-3 w-3 text-zinc-600 shrink-0 hover:text-zinc-400 cursor-pointer transition-colors" 
              />
            </div>

            <div className="shrink-0">
              <Globe className="h-4 w-4 text-zinc-600" />
            </div>
          </div>

          {/* Browser Iframe Canvas */}
          <div className="flex-1 min-h-[600px] bg-white overflow-hidden relative flex">
            <iframe 
              key={key}
              src={deployment.deploymentUrl} 
              className="w-full h-full min-h-[600px] border-0" 
              title={deployment.projectName}
              sandbox="allow-scripts allow-same-origin allow-forms"
            />
          </div>
        </div>
      </div>
    </Layout>
  );
};
export default Preview;
