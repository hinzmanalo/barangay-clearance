'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { AxiosError } from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      const { role, mustChangePassword } = await login(data);
      if (mustChangePassword) {
        router.push('/change-password');
        return;
      }
      if (role === 'RESIDENT') {
        router.push('/portal/dashboard');
      } else {
        router.push('/backoffice/dashboard');
      }
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Login failed. Please try again.');
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F8FAFC]">
      {/* Left Panel - Hidden on mobile, visible on md and above */}
      <div className="hidden md:flex w-1/2 bg-gradient-to-br from-[#062040] to-[#0A4F8F] flex-col items-center justify-center p-12 relative overflow-hidden">
        {/* Decorative dot-grid pattern (top-right, opacity-5) */}
        <div className="absolute top-0 right-0 w-40 h-40 opacity-5">
          <div className="grid grid-cols-10 gap-2">
            {Array.from({ length: 100 }).map((_, i) => (
              <div key={i} className="w-1 h-1 bg-white rounded-full" />
            ))}
          </div>
        </div>

        {/* Content */}
        <motion.div
          className="text-center z-10"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
        >
          {/* Barangay seal — Shield icon */}
          <Shield className="w-16 h-16 text-white/80 mb-6 mx-auto" strokeWidth={1.5} />

          {/* App name */}
          <h1 className="font-sora font-bold text-4xl text-white leading-tight">
            Barangay
            <br />
            Clearance System
          </h1>

          {/* Tagline */}
          <p className="font-geist text-base text-blue-200 mt-4 text-center max-w-xs">
            Ang serbisyo ng barangay,
            <br />
            nasa dulo ng iyong daliri.
          </p>
        </motion.div>
      </div>

      {/* Right Panel - Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: 'easeOut' }}
          className="w-full max-w-md"
        >
          <Card className="shadow-lg rounded-2xl p-8 bg-white">
            <h2 className="font-sora font-bold text-2xl text-neutral-900 mb-1">
              Welcome back
            </h2>
            <p className="font-geist text-sm text-neutral-500 mb-8">
              Sign in to your account
            </p>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {/* Email Input */}
              <Input
                label="Email address"
                type="email"
                placeholder=" "
                autoComplete="email"
                {...register('email')}
                error={errors.email?.message}
              />

              {/* Password Input */}
              <Input
                label="Password"
                type="password"
                placeholder=" "
                autoComplete="current-password"
                {...register('password')}
                error={errors.password?.message}
              />

              {/* Global error message with animation */}
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

              {/* Sign In Button */}
              <Button
                variant="primary"
                size="lg"
                className="w-full"
                loading={isSubmitting}
                type="submit"
              >
                Sign In
              </Button>
            </form>

            {/* Register Link */}
            <p className="mt-6 text-center font-geist text-sm text-neutral-500">
              New resident?{' '}
              <Link
                href="/register"
                className="text-primary-600 font-medium hover:underline transition-colors"
              >
                Register here
              </Link>
            </p>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}
