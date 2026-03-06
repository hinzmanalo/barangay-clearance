'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import {
  useBarangaySettings,
  useUpdateBarangaySettings,
  useUploadLogo,
} from '@/hooks/useSettings';
import { AxiosError } from 'axios';
import api from '@/lib/api';
import { toast } from '@/components/shared/ErrorToast';
import { Skeleton } from '@/components/ui/Skeleton';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Upload } from 'lucide-react';

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
      <div className="mx-auto max-w-3xl px-4 py-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="space-y-4">
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
    <div className="mx-auto max-w-3xl px-4 py-8 space-y-8">
      {/* Header */}
      <PageHeader
        title="Barangay Settings"
        description="Configure the barangay profile used on clearance documents."
      />

      {/* Profile form */}
      <Card className="p-6">
        <h2 className="text-lg font-semibold text-neutral-900 mb-6">Barangay Profile</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Barangay Name</label>
            <Input
              {...register('barangayName')}
              error={errors.barangayName?.message}
              placeholder="e.g. Barangay San Jose"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Municipality</label>
            <Input
              {...register('municipality')}
              error={errors.municipality?.message}
              placeholder="e.g. Municipality of Sample"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Province</label>
            <Input
              {...register('province')}
              error={errors.province?.message}
              placeholder="e.g. Province of Sample"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Barangay Captain</label>
            <Input
              {...register('captainName')}
              error={errors.captainName?.message}
              placeholder="e.g. Juan dela Cruz"
              required
            />
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              loading={isSubmitting || updateMutation.isPending}
            >
              {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Settings'}
            </Button>
          </div>
        </form>
      </Card>

      {/* Logo upload */}
      <Card className="p-6">
        <h2 className="text-lg font-semibold text-neutral-900 mb-4">Barangay Logo</h2>
        <p className="text-sm text-neutral-500 mb-6">
          Upload a PNG, JPEG, or GIF logo (max 2 MB). The logo appears on generated clearance PDFs.
        </p>

        {/* Preview */}
        {logoPreviewUrl && (
          <div className="mb-6">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={logoPreviewUrl}
              alt="Barangay logo preview"
              className="h-24 w-auto rounded border border-neutral-200 object-contain"
            />
            {logoFile && (
              <p className="mt-2 text-xs text-neutral-400">
                Selected: {logoFile.name} ({(logoFile.size / 1024).toFixed(1)} KB) — not yet uploaded
              </p>
            )}
          </div>
        )}

        {/* File input */}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg,image/gif"
          onChange={handleFileChange}
          className="hidden"
          id="logo-file-input"
        />

        <div className="flex items-center gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="w-4 h-4 mr-2" />
            {settings?.hasLogo ? 'Replace Logo' : 'Choose Logo'}
          </Button>

          {logoFile && (
            <Button
              type="button"
              variant="primary"
              loading={uploadLogoMutation.isPending}
              onClick={handleLogoUpload}
            >
              {uploadLogoMutation.isPending ? 'Uploading…' : 'Upload Logo'}
            </Button>
          )}
        </div>

        {logoError && <p className="mt-3 text-xs text-red-600">{logoError}</p>}

        {!settings?.hasLogo && !logoFile && (
          <p className="mt-3 text-xs text-neutral-400">No logo uploaded yet.</p>
        )}
      </Card>

      {/* Last updated */}
      {settings?.updatedAt && (
        <p className="text-xs text-neutral-400">
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
