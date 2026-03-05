'use client';

import { useState, useEffect } from 'react';
import { Menu, LogOut } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import BackofficeSidebar from '@/components/backoffice/Sidebar';
import { useRouter } from 'next/navigation';

export default function BackofficeLayout({ children }: { children: React.ReactNode }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { email, role, logout, mustChangePassword, isLoading } = useAuth();
  const router = useRouter();

  // Redirect to change-password if the admin / staff must change their password
  useEffect(() => {
    if (!isLoading && mustChangePassword) {
      router.replace('/change-password');
    }
  }, [isLoading, mustChangePassword, router]);

  const handleLogout = async () => {
    await logout();
    router.push('/login');
  };

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <BackofficeSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 shadow-sm">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-md p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-700 lg:hidden"
            aria-label="Open menu"
          >
            <Menu size={20} />
          </button>

          {/* Spacer on desktop */}
          <div className="hidden lg:block" />

          {/* User info + logout */}
          <div className="flex items-center gap-3">
            {email && (
              <div className="flex items-center gap-2">
                <span className="hidden text-sm text-gray-600 sm:block">{email}</span>
                {role && (
                  <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                    {role}
                  </span>
                )}
              </div>
            )}
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 hover:text-red-700"
              title="Log out"
            >
              <LogOut size={15} />
              <span className="hidden sm:inline">Log out</span>
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
