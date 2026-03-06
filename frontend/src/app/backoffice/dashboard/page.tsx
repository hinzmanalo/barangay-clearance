'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import {
  Clock,
  CheckCircle,
  FileCheck,
  LayoutList,
  FilePlus,
  ClipboardList,
  Users,
} from 'lucide-react';
import { useClearanceSummary, useClearances } from '@/hooks/useClearances';
import { StatCard } from '@/components/ui/StatCard';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { staggerContainer, staggerItem } from '@/lib/animations';

export default function BackofficeDashboardPage() {
  const { data: summary, isLoading } = useClearanceSummary({ refetchInterval: 30_000 });
  const { data: recentData } = useClearances({ page: 0, size: 5 });
  const [lastUpdated, setLastUpdated] = useState(0);

  useEffect(() => {
    if (!isLoading && summary) {
      setLastUpdated(0);
    }

    const interval = setInterval(() => {
      setLastUpdated((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [isLoading, summary]);

  const recentClearances = recentData?.content ?? [];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-8">
      {/* Page title */}
      <div>
        <h1 className="font-sora font-bold text-2xl text-neutral-900 mb-1">Dashboard</h1>
        <p className="font-geist text-sm text-neutral-500">Welcome back. Here's your clearance summary.</p>
      </div>

      {/* Stat cards (4-up grid) */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-lg border border-neutral-200 p-6">
              <Skeleton className="h-4 w-20 mb-3" />
              <Skeleton className="h-8 w-12" />
            </div>
          ))}
        </div>
      ) : summary ? (
        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={staggerItem}>
            <StatCard
              label="Pending"
              value={summary.pendingApproval}
              icon={Clock}
              accentColor="amber"
            />
          </motion.div>
          <motion.div variants={staggerItem}>
            <StatCard
              label="Approved"
              value={summary.approved}
              icon={CheckCircle}
              accentColor="teal"
            />
          </motion.div>
          <motion.div variants={staggerItem}>
            <StatCard
              label="Released"
              value={summary.releasedToday}
              icon={FileCheck}
              accentColor="blue"
            />
          </motion.div>
          <motion.div variants={staggerItem}>
            <StatCard
              label="Awaiting Payment"
              value={summary.awaitingPayment}
              icon={LayoutList}
              accentColor="blue"
            />
          </motion.div>
        </motion.div>
      ) : null}

      {/* Quick action cards (3-up grid) */}
      <motion.div
        className="grid grid-cols-1 sm:grid-cols-3 gap-4"
        variants={{
          hidden: {},
          visible: {
            transition: {
              staggerChildren: 0.08,
              delayChildren: 0.3,
            },
          },
        }}
        initial="hidden"
        animate="visible"
      >
        <motion.div variants={staggerItem}>
          <Link href="/backoffice/clearances/new">
            <Card hover className="group h-full flex flex-col items-start gap-3">
              <div className="p-3 rounded-lg bg-blue-100 group-hover:bg-blue-200 transition-colors">
                <FilePlus className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <h3 className="font-sora font-semibold text-neutral-900">New Walk-in Request</h3>
                <p className="font-geist text-xs text-neutral-500 mt-1">Create a clearance request</p>
              </div>
              <div className="mt-auto text-xs text-blue-600 group-hover:text-blue-700 transition-colors">
                Open →
              </div>
            </Card>
          </Link>
        </motion.div>

        <motion.div variants={staggerItem}>
          <Link href="/backoffice/clearances">
            <Card hover className="group h-full flex flex-col items-start gap-3">
              <div className="p-3 rounded-lg bg-teal-100 group-hover:bg-teal-200 transition-colors">
                <ClipboardList className="w-5 h-5 text-teal-600" />
              </div>
              <div>
                <h3 className="font-sora font-semibold text-neutral-900">View Clearances</h3>
                <p className="font-geist text-xs text-neutral-500 mt-1">Review all requests</p>
              </div>
              <div className="mt-auto text-xs text-teal-600 group-hover:text-teal-700 transition-colors">
                Open →
              </div>
            </Card>
          </Link>
        </motion.div>

        <motion.div variants={staggerItem}>
          <Link href="/backoffice/residents">
            <Card hover className="group h-full flex flex-col items-start gap-3">
              <div className="p-3 rounded-lg bg-purple-100 group-hover:bg-purple-200 transition-colors">
                <Users className="w-5 h-5 text-purple-600" />
              </div>
              <div>
                <h3 className="font-sora font-semibold text-neutral-900">Manage Residents</h3>
                <p className="font-geist text-xs text-neutral-500 mt-1">View resident registry</p>
              </div>
              <div className="mt-auto text-xs text-purple-600 group-hover:text-purple-700 transition-colors">
                Open →
              </div>
            </Card>
          </Link>
        </motion.div>
      </motion.div>

      {/* Recent clearances */}
      <Card>
        <div className="flex items-center justify-between mb-6">
          <h2 className="font-sora font-semibold text-lg text-neutral-900">Recent Clearances</h2>
          <Link href="/backoffice/clearances">
            <Button variant="ghost" size="sm">
              View all →
            </Button>
          </Link>
        </div>

        {recentClearances.length === 0 ? (
          <p className="text-center text-sm text-neutral-500 py-8">No clearances yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-t border-neutral-100">
                  <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
                    Resident
                  </th>
                  <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
                    Status
                  </th>
                  <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
                    Date
                  </th>
                </tr>
              </thead>
              <tbody>
                <motion.tr
                  className="contents"
                  variants={staggerContainer}
                  initial="hidden"
                  animate="visible"
                >
                  {recentClearances.map((cr) => (
                    <motion.tr
                      key={cr.id}
                      className="border-t border-neutral-50 hover:bg-neutral-50 transition-colors"
                      variants={staggerItem}
                    >
                      <td className="px-4 py-3 text-neutral-900 font-medium">
                        <Link href={`/backoffice/clearances/${cr.id}`} className="hover:text-blue-600">
                          {cr.residentName}
                        </Link>
                      </td>
                      <td className="px-4 py-3 text-neutral-600">{cr.status}</td>
                      <td className="px-4 py-3 text-neutral-500 text-xs">
                        {new Date(cr.createdAt).toLocaleDateString('en-PH')}
                      </td>
                    </motion.tr>
                  ))}
                </motion.tr>
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Last updated indicator */}
      <p className="font-geist text-xs text-neutral-400 text-center">
        Last updated{' '}
        {lastUpdated === 0 ? 'now' : lastUpdated < 60 ? `${lastUpdated}s ago` : 'recently'}
      </p>
    </div>
  );
}
