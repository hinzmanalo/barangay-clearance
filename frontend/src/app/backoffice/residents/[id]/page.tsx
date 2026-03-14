'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  useResident,
  useUpdateResident,
  useActivateResident,
  useRejectResident,
} from '@/hooks/useResidents';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { AxiosError } from 'axios';
import { toast } from '@/components/shared/ErrorToast';

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

  const showToast = (message: string, type: 'success' | 'error') => {
    if (type === 'success') toast.success(message);
    else toast.error(message);
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
    // Only allow submission when in edit mode
    if (!isEditing) return;
    
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
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 space-y-8">
        <Skeleton className="h-8 w-64" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <Skeleton className="h-96" />
          </div>
          <div>
            <Skeleton className="h-40" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !resident) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-700">Resident not found.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title={`${resident.lastName}, ${resident.firstName}`}
        backHref="/backoffice/residents"
      />

      {/* Two-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column: Form */}
        <div className="lg:col-span-2">
          <Card className="p-6">
            {serverError && (
              <div className="rounded-lg bg-red-50 border border-red-200 p-4 mb-6">
                <p className="font-geist text-sm text-red-700">{serverError}</p>
              </div>
            )}

            <form 
              onSubmit={(e) => {
                e.preventDefault();
                if (isEditing) {
                  handleSubmit(onSubmit)(e);
                }
              }}
              className="space-y-6"
            >
              {/* First name + Last name */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">First Name</label>
                  <Input
                    {...register('firstName')}
                    disabled={!isEditing}
                    error={errors.firstName?.message}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Last Name</label>
                  <Input
                    {...register('lastName')}
                    disabled={!isEditing}
                    error={errors.lastName?.message}
                  />
                </div>
              </div>

              {/* Middle name */}
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Middle Name</label>
                <Input
                  {...register('middleName')}
                  disabled={!isEditing}
                  error={errors.middleName?.message}
                />
              </div>

              {/* Birth date + gender */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Birth Date</label>
                  <Input
                    type="date"
                    {...register('birthDate')}
                    disabled={!isEditing}
                    error={errors.birthDate?.message}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Gender</label>
                  <Select
                    {...register('gender')}
                    disabled={!isEditing}
                    error={errors.gender?.message}
                  >
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </Select>
                </div>
              </div>

              {/* Address */}
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Address</label>
                <Textarea
                  {...register('address')}
                  disabled={!isEditing}
                  rows={3}
                  error={errors.address?.message}
                />
              </div>

              {/* Contact number + email */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Contact Number</label>
                  <Input
                    type="tel"
                    {...register('contactNumber')}
                    disabled={!isEditing}
                    error={errors.contactNumber?.message}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Email</label>
                  <Input
                    type="email"
                    {...register('email')}
                    disabled={!isEditing}
                    error={errors.email?.message}
                  />
                </div>
              </div>

              {/* Status */}
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Status</label>
                <Select
                  {...register('status')}
                  disabled={!isEditing}
                  error={errors.status?.message}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                </Select>
              </div>

              {/* Form actions */}
              <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
                {!isEditing ? (
                  <Button variant="primary" size="sm" type="button" onClick={() => setIsEditing(true)}>
                    Edit
                  </Button>
                ) : (
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      type="button"
                      onClick={() => {
                        setIsEditing(false);
                        setServerError(null);
                        reset();
                      }}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      type="submit"
                      loading={isSubmitting || updateMutation.isPending}
                      disabled={!isDirty || isSubmitting || updateMutation.isPending}
                    >
                      {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Changes'}
                    </Button>
                  </>
                )}
              </div>
            </form>
          </Card>
        </div>

        {/* Right column: Portal account panel */}
        {resident.hasPortalAccount && (
          <Card accentColor="teal">
            <h3 className="font-sora font-semibold text-base text-neutral-900 mb-4">
              Portal Account
            </h3>
            <div className="space-y-4 mb-4">
              <div>
                <p className="font-geist text-xs font-medium text-neutral-500 uppercase tracking-wide mb-2">
                  Status
                </p>
                {resident.portalStatus ? (
                  <Badge variant="user-status" value={resident.portalStatus} dot />
                ) : (
                  <Badge variant="user-status" value="INACTIVE" dot />
                )}
              </div>
              <p className="font-geist text-sm text-neutral-600">
                This resident has a linked portal account.
              </p>
            </div>
            <div className="space-y-2">
              {resident.portalStatus !== 'ACTIVE' && (
                <Button
                  variant="success"
                  size="sm"
                  className="w-full"
                  onClick={handleActivate}
                  loading={activateMutation.isPending}
                  disabled={activateMutation.isPending}
                >
                  Activate
                </Button>
              )}
              <Button
                variant="danger"
                size="sm"
                className="w-full"
                onClick={handleReject}
                loading={rejectMutation.isPending}
                disabled={rejectMutation.isPending}
              >
                Reject
              </Button>
            </div>
          </Card>
        )}
      </div>

      {/* Metadata */}
      <div className="text-xs text-neutral-400 space-y-1">
        <p>Created: {new Date(resident.createdAt).toLocaleString()}</p>
        <p>Last updated: {new Date(resident.updatedAt).toLocaleString()}</p>
        {resident.userId && <p>Linked user ID: {resident.userId}</p>}
      </div>
    </div>
  );
}
