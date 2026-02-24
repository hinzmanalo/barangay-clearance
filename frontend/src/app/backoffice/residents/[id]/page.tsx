'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';
import {
  useResident,
  useUpdateResident,
  useActivateResident,
  useRejectResident,
} from '@/hooks/useResidents';
import { AxiosError } from 'axios';

const updateResidentSchema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100),
  middleName: z.string().max(100).optional(),
  lastName: z.string().min(1, 'Last name is required').max(100),
  birthDate: z.string().min(1, 'Birth date is required'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']),
  address: z.string().min(1, 'Address is required'),
  contactNumber: z.string().max(20).optional(),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  status: z.enum(['ACTIVE', 'INACTIVE']),
});

type FormData = z.infer<typeof updateResidentSchema>;

export default function ResidentDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params?.id ?? '';

  const { data: resident, isLoading, error } = useResident(id);
  const updateMutation = useUpdateResident(id);
  const activateMutation = useActivateResident();
  const rejectMutation = useRejectResident();

  const [isEditing, setIsEditing] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(updateResidentSchema),
  });

  // Populate form when data loads
  useEffect(() => {
    if (resident) {
      reset({
        firstName: resident.firstName,
        middleName: resident.middleName ?? '',
        lastName: resident.lastName,
        birthDate: resident.birthDate,
        gender: resident.gender,
        address: resident.address,
        contactNumber: resident.contactNumber ?? '',
        email: resident.email ?? '',
        status: resident.status,
      });
    }
  }, [resident, reset]);

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      await updateMutation.mutateAsync({
        firstName: data.firstName,
        middleName: data.middleName || undefined,
        lastName: data.lastName,
        birthDate: data.birthDate,
        gender: data.gender,
        address: data.address,
        contactNumber: data.contactNumber || undefined,
        email: data.email || undefined,
        status: data.status,
      });
      setIsEditing(false);
      showToast('Resident updated successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Failed to update resident.');
    }
  };

  const handleActivate = async () => {
    if (!resident?.userId) return;
    try {
      await activateMutation.mutateAsync(resident.userId);
      showToast('Account activated successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to activate.', 'error');
    }
  };

  const handleReject = async () => {
    if (!resident?.userId) return;
    try {
      await rejectMutation.mutateAsync(resident.userId);
      showToast('Account rejected.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to reject.', 'error');
    }
  };

  if (isLoading) {
    return <div className="max-w-2xl mx-auto px-4 py-12 text-center text-gray-500">Loading…</div>;
  }

  if (error || !resident) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 text-center text-red-600">
        Resident not found.
        <Link href="/backoffice/residents" className="ml-2 text-blue-600 hover:underline">
          Go back
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      {/* Toast */}
      {toast && (
        <div
          className={`fixed top-4 right-4 z-50 rounded-lg px-4 py-3 text-sm text-white shadow-lg ${
            toast.type === 'success' ? 'bg-green-600' : 'bg-red-600'
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* Breadcrumb + header */}
      <div className="flex items-center gap-3">
        <Link href="/backoffice/residents" className="text-sm text-gray-500 hover:text-gray-800">
          ← Residents
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-2xl font-bold text-gray-900">
          {resident.lastName}, {resident.firstName}
        </h1>
      </div>

      {/* Portal account status banner */}
      {resident.hasPortalAccount && (
        <div className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 flex items-center justify-between">
          <span className="text-sm text-blue-800">
            This resident has a linked portal account.
          </span>
          <div className="flex gap-2">
            <button
              onClick={handleActivate}
              disabled={activateMutation.isPending}
              className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
            >
              Activate
            </button>
            <button
              onClick={handleReject}
              disabled={rejectMutation.isPending}
              className="rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
            >
              Reject
            </button>
          </div>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="bg-white shadow rounded-lg p-6 space-y-5">
        {serverError && (
          <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
            {serverError}
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">First Name *</label>
            <input
              type="text"
              {...register('firstName')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            />
            {errors.firstName && <p className="mt-1 text-xs text-red-600">{errors.firstName.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Last Name *</label>
            <input
              type="text"
              {...register('lastName')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            />
            {errors.lastName && <p className="mt-1 text-xs text-red-600">{errors.lastName.message}</p>}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Middle Name</label>
          <input
            type="text"
            {...register('middleName')}
            disabled={!isEditing}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Birth Date *</label>
            <input
              type="date"
              {...register('birthDate')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            />
            {errors.birthDate && <p className="mt-1 text-xs text-red-600">{errors.birthDate.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Gender *</label>
            <select
              {...register('gender')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            >
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Address *</label>
          <textarea
            {...register('address')}
            rows={2}
            disabled={!isEditing}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
          />
          {errors.address && <p className="mt-1 text-xs text-red-600">{errors.address.message}</p>}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contact Number</label>
            <input
              type="tel"
              {...register('contactNumber')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              {...register('email')}
              disabled={!isEditing}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
            />
            {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
          <select
            {...register('status')}
            disabled={!isEditing}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-500"
          >
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          {!isEditing ? (
            <button
              type="button"
              onClick={() => setIsEditing(true)}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Edit
            </button>
          ) : (
            <>
              <button
                type="button"
                onClick={() => {
                  setIsEditing(false);
                  setServerError(null);
                  reset();
                }}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!isDirty || isSubmitting || updateMutation.isPending}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Changes'}
              </button>
            </>
          )}
        </div>
      </form>

      {/* Metadata */}
      <div className="text-xs text-gray-400 space-y-1">
        <p>Created: {new Date(resident.createdAt).toLocaleString()}</p>
        <p>Last updated: {new Date(resident.updatedAt).toLocaleString()}</p>
        {resident.userId && <p>Linked user ID: {resident.userId}</p>}
      </div>
    </div>
  );
}
