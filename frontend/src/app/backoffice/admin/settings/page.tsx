'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import {
  useBarangaySettings,
  useUpdateBarangaySettings,
  useUploadLogo,
} from '@/hooks/useSettings';
import { AxiosError } from 'axios';
import api from '@/lib/api';
import { toast } from '@/components/shared/ErrorToast';
import { Skeleton } from '@/components/shared/LoadingSkeleton';

// ── Zod schema ───────────────────────────────────────────────────────────

const settingsSchema = z.object({
  barangayName: z.string().min(1, 'Barangay name is required').max(255),
  municipality: z.string().min(1, 'Municipality is required').max(255),
  province: z.string().min(1, 'Province is required').max(255),
  captainName: z.string().min(1, 'Captain name is required').max(255),
});

type SettingsForm = z.infer<typeof settingsSchema>;

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/gif'];
const MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB

// ── Component ────────────────────────────────────────────────────────────

export default function SettingsPage() {
  const { role, isLoading: authLoading } = useAuth();
  const router = useRouter();

  // Redirect non-admins
  useEffect(() => {
    if (!authLoading && role !== 'ADMIN') {
      router.replace('/backoffice/dashboard');
    }
  }, [authLoading, role, router]);

  const { data: settings, isLoading } = useBarangaySettings();
  const updateMutation = useUpdateBarangaySettings();
  const uploadLogoMutation = useUploadLogo();

  const [logoPreviewUrl, setLogoPreviewUrl] = useState<string | null>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<SettingsForm>({
    resolver: zodResolver(settingsSchema),
  });

  // Populate form when settings load
  useEffect(() => {
    if (settings) {
      reset({
        barangayName: settings.barangayName,
        municipality: settings.municipality,
        province: settings.province,
        captainName: settings.captainName,
      });
    }
  }, [settings, reset]);

  // Load existing logo preview if one has been uploaded
  useEffect(() => {
    if (!settings?.hasLogo) return;

    let objectUrl: string | null = null;

    api
      .get('/api/v1/settings/logo', { responseType: 'blob' })
      .then((res) => {
        objectUrl = URL.createObjectURL(res.data as Blob);
        setLogoPreviewUrl(objectUrl);
      })
      .catch(() => {
        // No logo or not accessible — ignore
      });

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [settings?.hasLogo]);

  // Clean up local file preview on unmount / when file changes
  useEffect(() => {
    return () => {
      if (logoPreviewUrl && logoFile) {
        URL.revokeObjectURL(logoPreviewUrl);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [logoPreviewUrl]);

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    if (type === 'success') toast.success(message);
    else toast.error(message);
  }, []);

  // ── File selection ────────────────────────────────────────────────────

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setLogoError(null);

    if (!file) return;

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setLogoError('Invalid file type. Accepted: PNG, JPEG, GIF.');
      return;
    }

    if (file.size > MAX_SIZE_BYTES) {
      setLogoError('File too large. Maximum size is 2 MB.');
      return;
    }

    // Revoke previous local preview URL if any
    if (logoFile && logoPreviewUrl) {
      URL.revokeObjectURL(logoPreviewUrl);
    }

    const url = URL.createObjectURL(file);
    setLogoFile(file);
    setLogoPreviewUrl(url);
  };

  const handleLogoUpload = async () => {
    if (!logoFile) return;

    try {
      await uploadLogoMutation.mutateAsync(logoFile);
      setLogoFile(null); // Clear pending file — preview remains from server
      showToast('Logo uploaded successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to upload logo.', 'error');
    }
  };

  // ── Form submit ───────────────────────────────────────────────────────

  const onSubmit = async (data: SettingsForm) => {
    try {
      await updateMutation.mutateAsync(data);
      showToast('Settings saved successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to save settings.', 'error');
    }
  };

  // ── Render ────────────────────────────────────────────────────────────

  if (authLoading || isLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="space-y-1">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-9 w-full rounded-md" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (role !== 'ADMIN') return null;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Barangay Settings</h1>
          <p className="mt-1 text-sm text-gray-500">
            Configure the barangay profile used on clearance documents.
          </p>
        </div>
        <Link
          href="/backoffice/admin/settings/fees"
          className="text-sm text-blue-600 hover:underline"
        >
          Manage Fees →
        </Link>
      </div>

      {/* Profile form */}
      <section className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold text-gray-800 mb-4">Barangay Profile</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Barangay Name
            </label>
            <input
              {...register('barangayName')}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. Barangay San Jose"
            />
            {errors.barangayName && (
              <p className="mt-1 text-xs text-red-600">{errors.barangayName.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Municipality
            </label>
            <input
              {...register('municipality')}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. Municipality of Sample"
            />
            {errors.municipality && (
              <p className="mt-1 text-xs text-red-600">{errors.municipality.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Province</label>
            <input
              {...register('province')}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. Province of Sample"
            />
            {errors.province && (
              <p className="mt-1 text-xs text-red-600">{errors.province.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Barangay Captain
            </label>
            <input
              {...register('captainName')}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. Juan dela Cruz"
            />
            {errors.captainName && (
              <p className="mt-1 text-xs text-red-600">{errors.captainName.message}</p>
            )}
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting || updateMutation.isPending}
              className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Settings'}
            </button>
          </div>
        </form>
      </section>

      {/* Logo upload */}
      <section className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="text-base font-semibold text-gray-800 mb-4">Barangay Logo</h2>
        <p className="text-sm text-gray-500 mb-4">
          Upload a PNG, JPEG, or GIF logo (max 2 MB). The logo appears on generated clearance
          PDFs.
        </p>

        {/* Preview */}
        {logoPreviewUrl && (
          <div className="mb-4">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={logoPreviewUrl}
              alt="Barangay logo preview"
              className="h-24 w-auto rounded border border-gray-200 object-contain"
            />
            {logoFile && (
              <p className="mt-1 text-xs text-gray-400">
                Selected: {logoFile.name} ({(logoFile.size / 1024).toFixed(1)} KB) — not yet
                uploaded
              </p>
            )}
          </div>
        )}

        {/* File input */}
        <div className="flex items-center gap-3">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/png,image/jpeg,image/gif"
            onChange={handleFileChange}
            className="hidden"
            id="logo-file-input"
          />
          <label
            htmlFor="logo-file-input"
            className="cursor-pointer rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            {settings?.hasLogo ? 'Replace Logo' : 'Choose Logo'}
          </label>

          {logoFile && (
            <button
              type="button"
              onClick={handleLogoUpload}
              disabled={uploadLogoMutation.isPending}
              className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
            >
              {uploadLogoMutation.isPending ? 'Uploading…' : 'Upload Logo'}
            </button>
          )}
        </div>

        {logoError && <p className="mt-2 text-xs text-red-600">{logoError}</p>}

        {!settings?.hasLogo && !logoFile && (
          <p className="mt-2 text-xs text-gray-400">No logo uploaded yet.</p>
        )}
      </section>

      {/* Last updated */}
      {settings?.updatedAt && (
        <p className="text-xs text-gray-400">
          Last updated:{' '}
          {new Date(settings.updatedAt).toLocaleString('en-PH', {
            dateStyle: 'medium',
            timeStyle: 'short',
          })}
        </p>
      )}
    </div>
  );
}
