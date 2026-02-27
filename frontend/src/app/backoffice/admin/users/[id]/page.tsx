'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Link from 'next/link';
import {
  useUser,
  useUpdateUser,
  useUpdateUserRole,
  useActivateUser,
  useDeactivateUser,
  useAdminResetPassword,
} from '@/hooks/useUsers';
import RoleBadge from '@/components/shared/RoleBadge';
import { DetailPageSkeleton } from '@/components/shared/LoadingSkeleton';
import { AxiosError } from 'axios';
import { toast } from '@/components/shared/ErrorToast';
import type { StaffUser } from '@/types/auth';

// ── Schemas ────────────────────────────────────────────────────────────────

const updateProfileSchema = z.object({
  firstName: z.string().min(1, 'Required').max(100),
  lastName: z.string().min(1, 'Required').max(100),
  email: z.string().email('Invalid email address'),
});

const resetPasswordSchema = z.object({
  newPassword: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'Must contain an uppercase letter')
    .regex(/[0-9]/, 'Must contain a digit'),
});

type ProfileFormData = z.infer<typeof updateProfileSchema>;
type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

// ── Component ──────────────────────────────────────────────────────────────

export default function UserDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params?.id ?? '';

  const { data: user, isLoading, error } = useUser(id);

  const updateMutation = useUpdateUser(id);
  const updateRoleMutation = useUpdateUserRole(id);
  const activateMutation = useActivateUser(id);
  const deactivateMutation = useDeactivateUser(id);
  const resetPasswordMutation = useAdminResetPassword(id);

  const [isEditing, setIsEditing] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);

  const [selectedRole, setSelectedRole] = useState<'ADMIN' | 'CLERK' | 'APPROVER'>('CLERK');
  const [roleError, setRoleError] = useState<string | null>(null);

  const [showResetModal, setShowResetModal] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);

  // Profile form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors: profileErrors, isSubmitting: profileSubmitting },
  } = useForm<ProfileFormData>({
    resolver: zodResolver(updateProfileSchema),
  });

  // Reset password form
  const {
    register: registerReset,
    handleSubmit: handleResetSubmit,
    reset: resetPasswordForm,
    formState: { errors: resetErrors, isSubmitting: resetSubmitting },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
  });

  // Populate forms when user data loads
  useEffect(() => {
    if (user) {
      reset({ firstName: user.firstName, lastName: user.lastName, email: user.email });
      if (user.role !== 'RESIDENT') {
        setSelectedRole(user.role);
      }
    }
  }, [user, reset]);

  const handleProfileSubmit = async (data: ProfileFormData) => {
    setProfileError(null);
    try {
      await updateMutation.mutateAsync(data);
      setIsEditing(false);
      toast.success('Profile updated.');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setProfileError(axiosErr.response?.data?.message ?? 'Failed to update profile.');
    }
  };

  const handleRoleChange = async () => {
    setRoleError(null);
    try {
      await updateRoleMutation.mutateAsync({ role: selectedRole });
      toast.success('Role updated.');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setRoleError(axiosErr.response?.data?.message ?? 'Failed to update role.');
    }
  };

  const handleToggleActivation = async () => {
    try {
      if (user?.status === 'DEACTIVATED') {
        await activateMutation.mutateAsync();
        toast.success('User reactivated.');
      } else {
        await deactivateMutation.mutateAsync();
        toast.success('User deactivated.');
      }
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      toast.error(axiosErr.response?.data?.message ?? 'Action failed.');
    }
  };

  const handlePasswordReset = async (data: ResetPasswordFormData) => {
    setResetError(null);
    try {
      await resetPasswordMutation.mutateAsync({ newPassword: data.newPassword });
      setShowResetModal(false);
      resetPasswordForm();
      toast.success('Password reset. User must change password on next login.');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setResetError(axiosErr.response?.data?.message ?? 'Failed to reset password.');
    }
  };

  if (isLoading) return <DetailPageSkeleton />;

  if (error || !user) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8">
        <p className="text-red-600 text-sm">User not found or failed to load.</p>
        <Link href="/backoffice/admin/users" className="text-blue-600 hover:underline text-sm mt-2 inline-block">
          ← Back to Users
        </Link>
      </div>
    );
  }

  const isDeactivated = user.status === 'DEACTIVATED';

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-8">
      {/* Breadcrumb */}
      <div className="flex items-center gap-3">
        <Link href="/backoffice/admin/users" className="text-sm text-gray-500 hover:text-gray-800">
          ← Users
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-2xl font-bold text-gray-900">
          {user.firstName} {user.lastName}
        </h1>
        <RoleBadge role={user.role} className="ml-1" />
        {user.mustChangePassword && (
          <span className="inline-flex items-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
            Must change PW
          </span>
        )}
      </div>

      {/* Profile card */}
      <section className="bg-white shadow rounded-lg p-6 space-y-5">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900">Profile</h2>
          {!isEditing && (
            <button
              onClick={() => setIsEditing(true)}
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              Edit
            </button>
          )}
        </div>

        {isEditing ? (
          <form onSubmit={handleSubmit(handleProfileSubmit)} className="space-y-4">
            {profileError && (
              <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                {profileError}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                <input
                  {...register('firstName')}
                  className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
                {profileErrors.firstName && (
                  <p className="mt-1 text-xs text-red-600">{profileErrors.firstName.message}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                <input
                  {...register('lastName')}
                  className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
                {profileErrors.lastName && (
                  <p className="mt-1 text-xs text-red-600">{profileErrors.lastName.message}</p>
                )}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                {...register('email')}
                className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
              {profileErrors.email && (
                <p className="mt-1 text-xs text-red-600">{profileErrors.email.message}</p>
              )}
            </div>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setIsEditing(false);
                  reset({ firstName: user.firstName, lastName: user.lastName, email: user.email });
                }}
                className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={profileSubmitting}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {profileSubmitting ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </form>
        ) : (
          <dl className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="font-medium text-gray-500">First Name</dt>
              <dd className="mt-0.5 text-gray-900">{user.firstName}</dd>
            </div>
            <div>
              <dt className="font-medium text-gray-500">Last Name</dt>
              <dd className="mt-0.5 text-gray-900">{user.lastName}</dd>
            </div>
            <div className="col-span-2">
              <dt className="font-medium text-gray-500">Email</dt>
              <dd className="mt-0.5 text-gray-900">{user.email}</dd>
            </div>
            <div>
              <dt className="font-medium text-gray-500">Status</dt>
              <dd className="mt-0.5">
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    user.status === 'ACTIVE'
                      ? 'bg-green-100 text-green-700'
                      : user.status === 'DEACTIVATED'
                      ? 'bg-red-100 text-red-700'
                      : 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {user.status}
                </span>
              </dd>
            </div>
            <div>
              <dt className="font-medium text-gray-500">Created</dt>
              <dd className="mt-0.5 text-gray-900">
                {new Date(user.createdAt).toLocaleDateString('en-PH', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </dd>
            </div>
          </dl>
        )}
      </section>

      {/* Change role */}
      <section className="bg-white shadow rounded-lg p-6 space-y-4">
        <h2 className="text-base font-semibold text-gray-900">Role</h2>
        {roleError && (
          <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
            {roleError}
          </div>
        )}
        <div className="flex items-center gap-3">
          <select
            value={selectedRole}
            onChange={(e) => setSelectedRole(e.target.value as 'ADMIN' | 'CLERK' | 'APPROVER')}
            className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ADMIN">Admin</option>
            <option value="APPROVER">Approver</option>
            <option value="CLERK">Clerk</option>
          </select>
          <button
            onClick={handleRoleChange}
            disabled={updateRoleMutation.isPending || selectedRole === user.role}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {updateRoleMutation.isPending ? 'Updating…' : 'Update Role'}
          </button>
        </div>
      </section>

      {/* Activate / Deactivate */}
      <section className="bg-white shadow rounded-lg p-6 space-y-3">
        <h2 className="text-base font-semibold text-gray-900">Account Status</h2>
        <p className="text-sm text-gray-600">
          {isDeactivated
            ? 'This account is currently deactivated. The user cannot log in.'
            : 'Deactivating this account will immediately revoke access.'}
        </p>
        <button
          onClick={handleToggleActivation}
          disabled={activateMutation.isPending || deactivateMutation.isPending}
          className={`rounded-md px-4 py-2 text-sm font-medium text-white disabled:opacity-50 ${
            isDeactivated
              ? 'bg-green-600 hover:bg-green-700'
              : 'bg-red-600 hover:bg-red-700'
          }`}
        >
          {activateMutation.isPending || deactivateMutation.isPending
            ? 'Processing…'
            : isDeactivated
            ? 'Reactivate Account'
            : 'Deactivate Account'}
        </button>
      </section>

      {/* Reset password */}
      <section className="bg-white shadow rounded-lg p-6 space-y-3">
        <h2 className="text-base font-semibold text-gray-900">Password Reset</h2>
        <p className="text-sm text-gray-600">
          Force-set a new password for this user. They will be required to change it on next login
          and all active sessions will be invalidated.
        </p>
        <button
          onClick={() => setShowResetModal(true)}
          className="rounded-md bg-gray-800 px-4 py-2 text-sm font-medium text-white hover:bg-gray-900"
        >
          Reset Password
        </button>
      </section>

      {/* Reset password modal */}
      {showResetModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white shadow-xl p-6 space-y-4">
            <h3 className="text-lg font-semibold text-gray-900">Reset Password</h3>
            <p className="text-sm text-gray-600">
              Set a new temporary password for{' '}
              <strong>
                {user.firstName} {user.lastName}
              </strong>
              .
            </p>

            {resetError && (
              <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
                {resetError}
              </div>
            )}

            <form onSubmit={handleResetSubmit(handlePasswordReset)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  New Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  autoComplete="new-password"
                  {...registerReset('newPassword')}
                  className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
                <p className="mt-1 text-xs text-gray-500">
                  At least 8 characters, one uppercase letter, one digit.
                </p>
                {resetErrors.newPassword && (
                  <p className="mt-1 text-xs text-red-600">{resetErrors.newPassword.message}</p>
                )}
              </div>
              <div className="flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowResetModal(false);
                    setResetError(null);
                    resetPasswordForm();
                  }}
                  className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={resetSubmitting}
                  className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                >
                  {resetSubmitting ? 'Resetting…' : 'Reset Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
