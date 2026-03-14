"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, FilePlus, X, LogOut } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/Button";

const NAV_ITEMS = [
  { label: "My Requests", href: "/portal/dashboard", icon: Home },
  { label: "New Request", href: "/portal/requests/new", icon: FilePlus },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export default function PortalSidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { email, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

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
          "fixed inset-y-0 left-0 z-30 flex flex-col bg-gradient-to-b from-[#0D7A70] to-[#0D5FA6] border-r border-teal-600",
          "lg:static lg:z-auto",
          open ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        ].join(" ")}
        animate={{ width: collapsed ? 64 : 256 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        style={{ position: open && window.innerWidth < 1024 ? "absolute" : "relative" }}
      >
        {/* Logo / branding */}
        <div className="flex h-16 items-center justify-between px-4 border-b border-teal-700">
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
                <span className="text-xs font-normal text-teal-200">Resident Portal</span>
              </motion.span>
            )}
          </AnimatePresence>

          <button
            onClick={onClose}
            className="rounded p-1 text-teal-200 hover:text-white lg:hidden"
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
                  className="relative flex items-center gap-3 px-3 py-2 text-sm font-medium transition-colors text-teal-100 hover:text-white rounded-lg cursor-pointer"
                  whileHover={{ backgroundColor: "rgba(13, 122, 112, 0.3)" }}
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
                    <motion.div
                      layoutId="portal-active-nav"
                      className="absolute right-2 w-2 h-2 bg-white rounded-full"
                      transition={{ type: "spring", stiffness: 300, damping: 30 }}
                    />
                  )}
                </motion.div>
              </Link>
            );
          })}
        </nav>

        {/* Bottom user chip */}
        <div className="border-t border-teal-700 p-3">
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
                    <p className="text-xs text-teal-200 truncate">{email}</p>
                    <span className="text-xs inline-block px-2 py-0.5 rounded-full bg-teal-400/20 text-teal-100 font-medium mt-1">
                      Resident
                    </span>
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLogout}
                  className="w-full justify-start text-teal-100 hover:text-white"
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
              className="w-full p-2 text-teal-100 hover:text-white rounded hover:bg-teal-700"
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
