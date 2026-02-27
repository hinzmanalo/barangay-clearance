'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  FileText,
  Users,
  BarChart3,
  Settings,
  Banknote,
  UserCog,
  X,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

const NAV_ITEMS = [
  { label: 'Dashboard', href: '/backoffice/dashboard', icon: LayoutDashboard },
  { label: 'Clearances', href: '/backoffice/clearances', icon: FileText },
  { label: 'Residents', href: '/backoffice/residents', icon: Users },
  { label: 'Reports', href: '/backoffice/reports', icon: BarChart3 },
];

const ADMIN_NAV_ITEMS = [
  { label: 'Barangay Settings', href: '/backoffice/admin/settings', icon: Settings, exact: true },
  { label: 'Fee Configuration', href: '/backoffice/admin/settings/fees', icon: Banknote, exact: false },
  { label: 'User Accounts', href: '/backoffice/admin/users', icon: UserCog, exact: false },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export default function BackofficeSidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { role } = useAuth();

  const isActive = (href: string) => pathname === href || pathname.startsWith(href + '/');

  return (
    <>
      {/* Mobile backdrop */}
      {open && (
        <div
          className="fixed inset-0 z-20 bg-black/40 lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar panel */}
      <aside
        className={[
          'fixed inset-y-0 left-0 z-30 flex w-60 flex-col bg-gray-900 transition-transform duration-200',
          'lg:static lg:translate-x-0 lg:z-auto',
          open ? 'translate-x-0' : '-translate-x-full',
        ].join(' ')}
      >
        {/* Logo / branding */}
        <div className="flex h-16 items-center justify-between px-5 border-b border-gray-700">
          <span className="text-sm font-semibold text-white leading-tight">
            Barangay Clearance
            <br />
            <span className="text-xs font-normal text-gray-400">Back Office</span>
          </span>
          <button
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:text-white lg:hidden"
          >
            <X size={18} />
          </button>
        </div>

        {/* Nav links */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          {NAV_ITEMS.map(({ label, href, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              onClick={onClose}
              className={[
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive(href)
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-800 hover:text-white',
              ].join(' ')}
            >
              <Icon size={16} />
              {label}
            </Link>
          ))}

          {/* Admin-only section */}
          {role === 'ADMIN' && (
            <>
              <div className="pt-4 pb-1 px-3">
                <p className="text-xs font-semibold uppercase tracking-wider text-gray-500">
                  Admin
                </p>
              </div>
              {ADMIN_NAV_ITEMS.map(({ label, href, icon: Icon, exact }) => (
                <Link
                  key={href}
                  href={href}
                  onClick={onClose}
                  className={[
                    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                    (exact ? pathname === href : isActive(href))
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-300 hover:bg-gray-800 hover:text-white',
                  ].join(' ')}
                >
                  <Icon size={16} />
                  {label}
                </Link>
              ))}
            </>
          )}
        </nav>
      </aside>
    </>
  );
}
