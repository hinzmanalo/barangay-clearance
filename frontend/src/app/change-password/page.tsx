'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { AxiosError } from 'axios';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload } from '@/types/auth';
import { motion, AnimatePresence } from 'framer-motion';
import { AlertTriangle } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormData = z.infer<typeof schema>;

export default function ChangePasswordPage() {
  const router = useRouter();
  const { role, refreshAuth } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      const { data: tokenData } = await api.put('/api/v1/auth/change-password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });

      // Refresh tokens in storage and cookie
      if (tokenData.accessToken) {
        localStorage.setItem('accessToken', tokenData.accessToken);
        const payload = jwtDecode<JwtPayload>(tokenData.accessToken);
        document.cookie = `accessToken=${tokenData.accessToken}; path=/; SameSite=Lax; max-age=${payload.exp - Math.floor(Date.now() / 1000)}`;
      }
      if (tokenData.refreshToken) {
        localStorage.setItem('refreshToken', tokenData.refreshToken);
      }

      // Update auth context to reflect the new token state
      refreshAuth();

      const destination = role === 'RESIDENT' ? '/portal/dashboard' : '/backoffice/dashboard';
      router.push(destination);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Failed to change password.');
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, ease: 'easeOut' }}
      >
        <Card className="w-full max-w-md p-8 shadow-lg rounded-2xl bg-white">
          {/* Amber warning banner */}
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            transition={{ duration: 0.3, delay: 0.1 }}
            className="flex items-start gap-3 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 mb-6 overflow-hidden"
          >
            <AlertTriangle className="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0" />
            <p className="font-geist text-sm text-amber-800">
              For your security, please set a new password before continuing.
            </p>
          </motion.div>

          <h2 className="font-sora font-bold text-xl text-neutral-900 mb-6">
            Set new password
          </h2>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {/* Server Error Message */}
            <AnimatePresence>
              {serverError && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.2 }}
                  className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2 border border-red-200"
                >
                  {serverError}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Current Password Input */}
            <Input
              label="Current password"
              type="password"
              placeholder=" "
              autoComplete="current-password"
              {...register('currentPassword')}
              error={errors.currentPassword?.message}
            />

            {/* New Password Input */}
            <Input
              label="New password"
              type="password"
              placeholder=" "
              autoComplete="new-password"
              {...register('newPassword')}
              error={errors.newPassword?.message}
            />

            {/* Confirm Password Input */}
            <Input
              label="Confirm new password"
              type="password"
              placeholder=" "
              autoComplete="new-password"
              {...register('confirmPassword')}
              error={errors.confirmPassword?.message}
            />

            {/* Update Button */}
            <Button
              variant="primary"
              size="lg"
              className="w-full"
              loading={isSubmitting}
              type="submit"
            >
              Update password
            </Button>
          </form>
        </Card>
      </motion.div>
    </div>
  );
}
