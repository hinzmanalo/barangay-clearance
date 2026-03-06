'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import api from '@/lib/api';
import { AxiosError } from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, ChevronRight } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

const registerSchema = z
  .object({
    email: z.string().email('Invalid email address'),
    password: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string(),
    firstName: z.string().min(1, 'First name is required').max(100),
    middleName: z.string().max(100).optional(),
    lastName: z.string().min(1, 'Last name is required').max(100),
    birthDate: z.string().min(1, 'Birth date is required'),
    gender: z.enum(['MALE', 'FEMALE', 'OTHER'], { error: 'Gender is required' }),
    address: z.string().min(1, 'Address is required'),
    contactNumber: z.string().max(20).optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [serverError, setServerError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    watch,
    trigger,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
  });

  // Watch form values for step 1 validation
  const email = watch('email');
  const password = watch('password');
  const confirmPassword = watch('confirmPassword');

  // Validate step 1 and move to step 2
  const handleNextStep = async () => {
    const isValid = await trigger(['email', 'password', 'confirmPassword']);
    if (isValid) {
      setStep(2);
    }
  };

  // Go back to step 1
  const handlePrevStep = () => {
    setStep(1);
  };

  const onSubmit = async (data: RegisterFormData) => {
    setServerError(null);
    try {
      await api.post('/api/v1/auth/register', {
        email: data.email,
        password: data.password,
        firstName: data.firstName,
        middleName: data.middleName || undefined,
        lastName: data.lastName,
        birthDate: data.birthDate,
        gender: data.gender,
        address: data.address,
        contactNumber: data.contactNumber || undefined,
      });
      setSuccess(true);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Registration failed. Please try again.');
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC] px-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="w-full max-w-md"
        >
          <Card className="shadow-lg rounded-2xl p-8 bg-white">
            <div className="flex flex-col items-center text-center space-y-4">
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 100 }}
                className="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center"
              >
                <Check className="w-6 h-6 text-green-600" />
              </motion.div>
              <h2 className="font-sora font-bold text-xl text-neutral-900">
                Registration Successful
              </h2>
              <p className="font-geist text-sm text-neutral-600">
                Your account has been created and is pending verification by barangay staff.
                You will be able to log in once your account is activated.
              </p>
              <Link href="/login" className="text-primary-600 font-medium hover:underline text-sm">
                Back to Login
              </Link>
            </div>
          </Card>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F8FAFC] px-4 py-8">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: 'easeOut' }}
        className="w-full max-w-2xl"
      >
        <Card className="shadow-lg rounded-2xl bg-white overflow-hidden">
          {/* Header with branding */}
          <div className="bg-primary-700 py-4 px-8 flex items-center gap-3">
            <div className="w-7 h-7 rounded-lg bg-white/20 flex items-center justify-center">
              <span className="text-white font-bold text-sm">☐</span>
            </div>
            <span className="font-sora font-semibold text-lg text-white">
              Barangay Clearance System
            </span>
          </div>

          <div className="p-8">
            {/* 2-Step Stepper */}
            <div className="flex items-center justify-between mb-8">
              {[1, 2].map((stepNum) => (
                <div key={stepNum} className="flex items-center flex-1">
                  {/* Step Circle */}
                  <motion.div
                    className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm transition-all ${
                      step > stepNum
                        ? 'bg-teal-500 text-white'
                        : step === stepNum
                          ? 'bg-primary-700 text-white'
                          : 'bg-neutral-200 text-neutral-500'
                    }`}
                    animate={step > stepNum ? { scale: 1.1 } : { scale: 1 }}
                  >
                    {step > stepNum ? <Check size={16} /> : stepNum}
                  </motion.div>

                  {/* Step Label */}
                  <div className="ml-3 flex-1">
                    <p
                      className={`text-xs font-medium ${
                        step >= stepNum ? 'text-primary-700' : 'text-neutral-500'
                      }`}
                    >
                      {stepNum === 1 ? 'Account Info' : 'Personal Info'}
                    </p>
                  </div>

                  {/* Connector Line */}
                  {stepNum < 2 && (
                    <div
                      className={`h-0.5 flex-1 mx-2 transition-colors ${
                        step > stepNum ? 'bg-teal-500' : 'bg-neutral-200'
                      }`}
                    />
                  )}
                </div>
              ))}
            </div>

            {/* Server Error Message */}
            <AnimatePresence>
              {serverError && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2 border border-red-200 mb-6"
                >
                  {serverError}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Form - Step Content with Animation */}
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <AnimatePresence mode="wait">
                {step === 1 ? (
                  <motion.div
                    key="step1"
                    initial={{ opacity: 0, x: 30 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -30 }}
                    transition={{ duration: 0.25 }}
                    className="space-y-5"
                  >
                    <div>
                      <h3 className="font-sora font-semibold text-neutral-900 mb-4">
                        Create your account
                      </h3>
                    </div>

                    <Input
                      label="Email address"
                      type="email"
                      placeholder=" "
                      autoComplete="email"
                      {...register('email')}
                      error={errors.email?.message}
                    />

                    <Input
                      label="Password"
                      type="password"
                      placeholder=" "
                      autoComplete="new-password"
                      {...register('password')}
                      error={errors.password?.message}
                    />

                    <Input
                      label="Confirm password"
                      type="password"
                      placeholder=" "
                      autoComplete="new-password"
                      {...register('confirmPassword')}
                      error={errors.confirmPassword?.message}
                    />
                  </motion.div>
                ) : (
                  <motion.div
                    key="step2"
                    initial={{ opacity: 0, x: 30 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -30 }}
                    transition={{ duration: 0.25 }}
                    className="space-y-5"
                  >
                    <div>
                      <h3 className="font-sora font-semibold text-neutral-900 mb-4">
                        Tell us about yourself
                      </h3>
                    </div>

                    {/* Two-column grid on md: */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input
                        label="First name"
                        placeholder=" "
                        {...register('firstName')}
                        error={errors.firstName?.message}
                      />
                      <Input
                        label="Last name"
                        placeholder=" "
                        {...register('lastName')}
                        error={errors.lastName?.message}
                      />
                    </div>

                    <Input
                      label="Middle name (optional)"
                      placeholder=" "
                      {...register('middleName')}
                    />

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input
                        label="Date of birth"
                        type="date"
                        {...register('birthDate')}
                        error={errors.birthDate?.message}
                      />
                      <Select
                        label="Gender"
                        {...register('gender')}
                        error={errors.gender?.message}
                      >
                        <option value="">Select gender</option>
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                      </Select>
                    </div>

                    <Input
                      label="Street / House no."
                      placeholder=" "
                      {...register('address')}
                      error={errors.address?.message}
                    />

                    <Input
                      label="Contact number (optional)"
                      type="tel"
                      placeholder=" "
                      {...register('contactNumber')}
                    />
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Navigation Buttons */}
              <div className="flex justify-between gap-3 pt-2">
                {step === 2 && (
                  <Button
                    variant="ghost"
                    type="button"
                    onClick={handlePrevStep}
                  >
                    ← Back
                  </Button>
                )}
                <div className={step === 1 ? 'ml-auto' : ''}>
                  {step === 1 ? (
                    <Button
                      variant="primary"
                      type="button"
                      onClick={handleNextStep}
                      className="flex items-center gap-2"
                    >
                      Next
                      <ChevronRight size={16} />
                    </Button>
                  ) : (
                    <Button
                      variant="primary"
                      type="submit"
                      loading={isSubmitting}
                      className="flex items-center gap-2"
                    >
                      Create account
                    </Button>
                  )}
                </div>
              </div>
            </form>

            {/* Sign In Link */}
            <p className="mt-6 text-center font-geist text-sm text-neutral-500">
              Already have an account?{' '}
              <Link
                href="/login"
                className="text-primary-600 font-medium hover:underline transition-colors"
              >
                Sign in
              </Link>
            </p>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
