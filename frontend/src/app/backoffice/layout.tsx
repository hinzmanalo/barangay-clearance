"use client";

import { useState, useEffect } from "react";
import { Menu, LogOut } from "lucide-react";
import { motion } from "framer-motion";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import BackofficeSidebar from "@/components/backoffice/Sidebar";
import { useRouter } from "next/navigation";
import { Badge } from "@/components/ui/Badge";

export default function BackofficeLayout({ children }: { children: React.ReactNode }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { email, role, logout, mustChangePassword, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  // Redirect to change-password if the admin / staff must change their password
  useEffect(() => {
    if (!isLoading && mustChangePassword) {
      router.replace("/change-password");
    }
  }, [isLoading, mustChangePassword, router]);

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <div className="flex h-screen overflow-hidden bg-neutral-50">
      <BackofficeSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex h-16 shrink-0 items-center justify-between border-b border-neutral-200 bg-white px-4 shadow-sm">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-md p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 lg:hidden"
            aria-label="Open menu"
          >
            <Menu size={20} />
          </button>

          {/* Spacer on desktop */}
          <div className="hidden lg:block" />

          {/* User info + logout */}
          <div className="flex items-center gap-4">
            {email && (
              <div className="flex items-center gap-3">
                <span className="hidden text-sm text-neutral-600 sm:block">{email}</span>
                {role && <Badge variant="role" value={role} />}
              </div>
            )}
            <button
              onClick={handleLogout}
              className="rounded-md p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700"
              aria-label="Logout"
            >
              <LogOut size={20} />
            </button>
          </div>
        </header>

        {/* Main content with page transition */}
        <main className="flex-1 overflow-auto">
          <motion.div
            key={pathname}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, ease: "easeOut" }}
            className="p-6"
          >
            {children}
          </motion.div>
        </main>
      </div>
    </div>
  );
}
