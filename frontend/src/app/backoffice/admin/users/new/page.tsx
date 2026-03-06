'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useCreateUser } from '@/hooks/useUsers';
import { AxiosError } from 'axios';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';

const createStaffSchema = z.object({
  email: z.string().email('Invalid email address'),
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName: z.string().min(1, 'Last name is required').max(100),
  role: z.enum(['ADMIN', 'CLERK', 'APPROVER'], { message: 'Role is required' }),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[0-9]/, 'Password must contain at least one digit'),
});

type FormData = z.infer<typeof createStaffSchema>;

export default function NewUserPage() {
  const router = useRouter();
  const createMutation = useCreateUser();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(createStaffSchema),
    defaultValues: { role: 'CLERK' },
  });

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      const user = await createMutation.mutateAsync(data);
      router.push(`/backoffice/admin/users/${user.id}`);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Failed to create user.');
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 space-y-8">
      <PageHeader
        title="New Staff Account"
        description="Create a new staff account with temporary password."
        backHref="/backoffice/admin/users"
      />

      <Card className="p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {serverError && (
            <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
              {serverError}
            </div>
          )}

          {/* Name fields */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="First Name"
              {...register('firstName')}
              error={errors.firstName?.message}
              required
            />
            <Input
              label="Last Name"
              {...register('lastName')}
              error={errors.lastName?.message}
              required
            />
          </div>

          {/* Email */}
          <Input
            label="Email Address"
            type="email"
            {...register('email')}
            error={errors.email?.message}
            required
          />

          {/* Role */}
          <Select
            label="Role"
            {...register('role')}
            error={errors.role?.message}
            required
          >
            <option value="CLERK">Clerk</option>
            <option value="APPROVER">Approver</option>
            <option value="ADMIN">Admin</option>
          </Select>

          {/* Password */}
          <div>
            <Input
              label="Temporary Password"
              type="password"
              {...register('password')}
              error={errors.password?.message}
              required
            />
            <p className="mt-2 text-xs text-neutral-500">
              At least 8 characters, one uppercase letter, one digit. The user will be prompted to change it on first login.
            </p>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => router.push('/backoffice/admin/users')}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={isSubmitting}
            >
              {isSubmitting ? 'Creating…' : 'Create Account'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
