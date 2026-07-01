import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Activity, LayoutDashboard, FolderKanban, LogOut } from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const userJson = localStorage.getItem('user');
  const user = userJson ? JSON.parse(userJson) : null;

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col font-sans">
      {/* Top Header */}
      <header className="border-b border-border bg-card/50 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-8">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2 text-white font-bold tracking-tight text-lg">
              <Activity className="h-5 w-5 text-primary" />
              <span>DeployForge</span>
            </Link>

            {/* Nav */}
            <nav className="flex items-center space-x-4">
              <Link
                to="/"
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:text-white transition-colors"
              >
                <LayoutDashboard className="h-4 w-4" />
                <span>Dashboard</span>
              </Link>
              <Link
                to="/projects"
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-md text-sm font-medium text-muted-foreground hover:text-white transition-colors"
              >
                <FolderKanban className="h-4 w-4" />
                <span>Projects</span>
              </Link>
            </nav>
          </div>

          {/* Right Profile Controls */}
          {user && (
            <div className="flex items-center space-x-6">
              <div className="text-right hidden sm:block">
                <p className="text-xs font-semibold text-white">{user.name}</p>
                <p className="text-[10px] text-muted-foreground">{user.email}</p>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-1.5 px-3 py-1.5 text-xs text-rose-400 font-medium hover:bg-rose-950/20 border border-transparent hover:border-rose-900/30 rounded-md transition-all cursor-pointer"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
};
