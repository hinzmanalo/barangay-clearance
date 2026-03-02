'use client';

/**
 * Re-exports sonner's Toaster for placement in the root layout,
 * and a typed `toast` API for use throughout the application.
 *
 * Usage:
 *   import { toast } from '@/components/shared/ErrorToast';
 *   toast.success('Resident activated.');
 *   toast.error('Something went wrong.');
 */
export { Toaster } from 'sonner';
export { toast } from 'sonner';
