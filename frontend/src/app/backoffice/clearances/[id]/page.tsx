'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { Download } from 'lucide-react';
import ActionButtons from '@/components/backoffice/ActionButtons';
import { useClearance, downloadClearancePdf } from '@/hooks/useClearances';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import type { ClearanceRequest } from '@/types/clearance';
import { PURPOSE_LABELS } from '@/types/clearance';
import { toast } from '@/components/shared/ErrorToast';

export default function ClearanceDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { data: initialCr, isLoading } = useClearance(id);
  const [cr, setCr] = useState<ClearanceRequest | null>(null);
  const [downloading, setDownloading] = useState(false);

  // Use local state override after actions, fall back to query data
  const clearance = cr ?? initialCr;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-8 space-y-8">
        <Skeleton className="h-8 w-64" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <Skeleton className="h-40" />
            <Skeleton className="h-64" />
          </div>
          <div className="space-y-4">
            <Skeleton className="h-40" />
            <Skeleton className="h-32" />
          </div>
        </div>
      </div>
    );
  }

  if (!clearance) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-700">Request not found.</p>
        </div>
      </div>
    );
  }

  const purposeLabel =
    clearance.purpose === 'OTHER' && clearance.purposeOther
      ? clearance.purposeOther
      : PURPOSE_LABELS[clearance.purpose] ?? clearance.purpose;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title={clearance.residentName ?? 'Clearance Request'}
        description={`Clearance #${clearance.clearanceNumber || 'Pending'}`}
        backHref="/backoffice/clearances"
      />

      {/* Two-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column: Clearance info + Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Status badges */}
          <Card className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <Badge variant="status" value={clearance.status} dot />
              <Badge variant="payment" value={clearance.paymentStatus} dot />
              {clearance.clearanceNumber && (
                <span className="ml-auto font-mono text-sm text-neutral-600">
                  #{clearance.clearanceNumber}
                </span>
              )}
            </div>
          </Card>

          {/* Details grid */}
          <Card className="p-6">
            <h2 className="font-sora font-semibold text-lg text-neutral-900 mb-4">Details</h2>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Resident
                </dt>
                <dd className="font-medium text-neutral-900">{clearance.residentName ?? '—'}</dd>
              </div>
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Purpose
                </dt>
                <dd className="font-medium text-neutral-900">{purposeLabel}</dd>
              </div>
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Urgency
                </dt>
                <dd className="font-medium text-neutral-900">{clearance.urgency}</dd>
              </div>
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Copies
                </dt>
                <dd className="font-medium text-neutral-900">{clearance.copies}</dd>
              </div>
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Fee Amount
                </dt>
                <dd className="font-medium text-neutral-900">
                  ₱{Number(clearance.feeAmount).toFixed(2)}
                </dd>
              </div>
              <div>
                <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                  Date Submitted
                </dt>
                <dd className="font-medium text-neutral-900">
                  {new Date(clearance.createdAt).toLocaleDateString('en-PH', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </dd>
              </div>
              {clearance.reviewedAt && (
                <div>
                  <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                    Date Reviewed
                  </dt>
                  <dd className="font-medium text-neutral-900">
                    {new Date(clearance.reviewedAt).toLocaleDateString('en-PH', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </dd>
                </div>
              )}
              {clearance.issuedAt && (
                <div>
                  <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                    Date Released
                  </dt>
                  <dd className="font-medium text-neutral-900">
                    {new Date(clearance.issuedAt).toLocaleDateString('en-PH', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </dd>
                </div>
              )}
              {clearance.notes && (
                <div className="col-span-2">
                  <dt className="font-geist text-xs text-neutral-500 uppercase tracking-wide mb-1">
                    Notes
                  </dt>
                  <dd className="mt-2 text-neutral-900 whitespace-pre-wrap text-sm">
                    {clearance.notes}
                  </dd>
                </div>
              )}
            </dl>
          </Card>

          {/* Resident profile link */}
          <Link href={`/backoffice/residents/${clearance.residentId}`}>
            <Button variant="outline" className="w-full">
              View Resident Profile →
            </Button>
          </Link>
        </div>

        {/* Right column: Action panel + PDF download */}
        <div className="space-y-4">
          {/* Action buttons */}
          <Card accentColor="blue" className="p-6">
            <h3 className="font-sora font-semibold text-base text-neutral-900 mb-4">Actions</h3>
            <ActionButtons clearance={clearance} onSuccess={setCr} />
          </Card>

          {/* PDF download — shown when RELEASED */}
          {clearance.status === 'RELEASED' && clearance.clearanceNumber && (
            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Card className="p-4">
                <p className="font-geist text-xs text-neutral-500 mb-3 uppercase tracking-wide">
                  Document
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={async () => {
                    setDownloading(true);
                    try {
                      await downloadClearancePdf(clearance.id, clearance.clearanceNumber!);
                    } catch {
                      toast.error('Failed to download PDF. Please try again.');
                    } finally {
                      setDownloading(false);
                    }
                  }}
                  disabled={downloading}
                >
                  <Download className="w-4 h-4 mr-2" />
                  {downloading ? 'Downloading…' : 'Download PDF'}
                </Button>
              </Card>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
}
