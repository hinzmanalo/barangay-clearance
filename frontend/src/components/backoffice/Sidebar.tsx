"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  FileText,
  Users,
  BarChart3,
  Settings,
  Banknote,
  UserCog,
  ClipboardList,
  X,
  LogOut,
  ChevronDown,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";

const NAV_ITEMS = [
  { label: "Dashboard", href: "/backoffice/dashboard", icon: LayoutDashboard },
  { label: "Clearances", href: "/backoffice/clearances", icon: FileText },
  { label: "Residents", href: "/backoffice/residents", icon: Users },
  { label: "Reports", href: "/backoffice/reports", icon: BarChart3 },
];

const ADMIN_NAV_ITEMS = [
  { label: "Barangay Settings", href: "/backoffice/admin/settings", icon: Settings, exact: true },
  { label: "Fee Configuration", href: "/backoffice/admin/settings/fees", icon: Banknote, exact: false },
  { label: "User Accounts", href: "/backoffice/admin/users", icon: UserCog, exact: false },
  { label: "Audit Logs", href: "/backoffice/admin/audit-logs", icon: ClipboardList, exact: false },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export default function BackofficeSidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { role, email, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);

  const isActive = (href: string) => pathname === href || pathname.startsWith(href + "/");

  const handleLogout = async () => {
    await logout();
  };

  return (
    <>
      {/* Mobile backdrop */}
      {open && (
        <motion.div
          className="fixed inset-0 z-20 bg-black/40 lg:hidden"
          onClick={onClose}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        />
      )}

      {/* Sidebar panel */}
      <motion.aside
        className={[
          "fixed inset-y-0 left-0 z-30 flex flex-col bg-gradient-to-b from-[#0A1E3D] to-[#062040] border-r border-primary-800",
          "lg:static lg:z-auto",
          open ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        ].join(" ")}
        animate={{ width: collapsed ? 64 : 256 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        style={{ position: open && window.innerWidth < 1024 ? "absolute" : "relative" }}
      >
        {/* Logo / branding */}
        <div className="flex h-16 items-center justify-between px-4 border-b border-primary-700">
          <AnimatePresence>
            {!collapsed && (
              <motion.span
                className="text-sm font-semibold text-white leading-tight"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                Barangay Clearance
                <br />
                <span className="text-xs font-normal text-primary-300">Back Office</span>
              </motion.span>
            )}
          </AnimatePresence>

          <button
            onClick={onClose}
            className="rounded p-1 text-primary-300 hover:text-white lg:hidden"
          >
            <X size={18} />
          </button>
        </div>

        {/* Nav links */}
        <nav className="flex-1 overflow-y-auto px-2 py-4 space-y-1 relative">
          {NAV_ITEMS.map(({ label, href, icon: Icon }) => {
            const active = isActive(href);
            return (
              <Link key={href} href={href} onClick={onClose}>
                <motion.div
                  className="relative flex items-center gap-3 px-3 py-2 text-sm font-medium transition-colors text-primary-200 hover:text-white rounded-lg cursor-pointer"
                  whileHover={{ backgroundColor: "rgba(15, 79, 143, 0.2)" }}
                >
                  <Icon size={18} />
                  <AnimatePresence>
                    {!collapsed && (
                      <motion.span
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="whitespace-nowrap"
                      >
                        {label}
                      </motion.span>
                    )}
                  </AnimatePresence>

                  {active && (
                    <motion.span
                      layoutId="backoffice-active-nav"
                      className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-teal-400 rounded-r"
                      transition={{ type: "spring", stiffness: 300, damping: 30 }}
                    />
                  )}
                </motion.div>
              </Link>
            );
          })}

          {/* Admin-only section */}
          {role === "ADMIN" && (
            <>
              <button
                onClick={() => setAdminOpen(!adminOpen)}
                className="w-full flex items-center gap-3 px-3 py-2 text-sm font-medium transition-colors text-primary-200 hover:text-white rounded-lg mt-6 pt-4 border-t border-primary-700"
              >
                <Settings size={18} />
                <AnimatePresence>
                  {!collapsed && (
                    <motion.div
                      className="flex items-center justify-between flex-1 whitespace-nowrap"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                    >
                      <span>Admin</span>
                      <motion.div animate={{ rotate: adminOpen ? 180 : 0 }}>
                        <ChevronDown size={16} />
                      </motion.div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </button>

              <AnimatePresence>
                {adminOpen && (
                  <motion.div
                    className="space-y-1 mt-2"
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                  >
                    {ADMIN_NAV_ITEMS.map(({ label, href, icon: Icon, exact }) => {
                      const active = exact ? pathname === href : isActive(href);
                      return (
                        <Link key={href} href={href} onClick={onClose}>
                          <motion.div
                            className="relative flex items-center gap-3 px-3 py-2 text-sm font-medium transition-colors text-primary-200 hover:text-white rounded-lg cursor-pointer ml-4"
                            whileHover={{ backgroundColor: "rgba(15, 79, 143, 0.2)" }}
                          >
                            <Icon size={18} />
                            <AnimatePresence>
                              {!collapsed && (
                                <motion.span
                                  initial={{ opacity: 0 }}
                                  animate={{ opacity: 1 }}
                                  exit={{ opacity: 0 }}
                                  className="whitespace-nowrap"
                                >
                                  {label}
                                </motion.span>
                              )}
                            </AnimatePresence>

                            {active && (
                              <motion.span
                                layoutId="backoffice-active-nav"
                                className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-teal-400 rounded-r"
                                transition={{ type: "spring", stiffness: 300, damping: 30 }}
                              />
                            )}
                          </motion.div>
                        </Link>
                      );
                    })}
                  </motion.div>
                )}
              </AnimatePresence>
            </>
          )}
        </nav>

        {/* Bottom user chip */}
        <div className="border-t border-primary-700 p-3">
          <AnimatePresence>
            {!collapsed && (
              <motion.div
                className="space-y-3"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <div className="flex items-center justify-between gap-2 px-3 py-2">
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-primary-300 truncate">{email}</p>
                    <Badge variant="role" value={role || "RESIDENT"} />
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLogout}
                  className="w-full justify-start text-primary-200 hover:text-white"
                >
                  <LogOut size={16} />
                  Logout
                </Button>
              </motion.div>
            )}
          </AnimatePresence>

          {collapsed && (
            <button
              onClick={handleLogout}
              className="w-full p-2 text-primary-200 hover:text-white rounded hover:bg-primary-700"
              title="Logout"
            >
              <LogOut size={18} />
            </button>
          )}
        </div>
      </motion.aside>
    </>
  );
}
