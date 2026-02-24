import { NextRequest, NextResponse } from 'next/server';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload } from '@/types/auth';

// Public routes that don't require authentication
const PUBLIC_ROUTES = ['/login', '/register'];

// Routes only accessible to specific roles
const BACKOFFICE_ROLES = ['ADMIN', 'CLERK', 'APPROVER'];

function getTokenFromCookieOrHeader(request: NextRequest): string | null {
  // For SSR middleware, tokens are typically passed as cookies
  // In MVP we rely on localStorage (client-side only), so middleware
  // can only check cookies named 'accessToken' if set.
  return request.cookies.get('accessToken')?.value ?? null;
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Allow public routes
  if (PUBLIC_ROUTES.some((route) => pathname.startsWith(route))) {
    return NextResponse.next();
  }

  const token = getTokenFromCookieOrHeader(request);

  // No token — redirect to login
  if (!token) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('next', pathname);
    return NextResponse.redirect(loginUrl);
  }

  try {
    const payload = jwtDecode<JwtPayload>(token);

    // Token expired
    if (payload.exp * 1000 < Date.now()) {
      const loginUrl = new URL('/login', request.url);
      loginUrl.searchParams.set('next', pathname);
      return NextResponse.redirect(loginUrl);
    }

    // Backoffice routes — require staff role
    if (pathname.startsWith('/backoffice') && !BACKOFFICE_ROLES.includes(payload.role)) {
      return NextResponse.redirect(new URL('/portal/dashboard', request.url));
    }

    // Portal routes — require RESIDENT role
    if (pathname.startsWith('/portal') && payload.role !== 'RESIDENT') {
      return NextResponse.redirect(new URL('/backoffice/dashboard', request.url));
    }

    return NextResponse.next();
  } catch {
    // Invalid token
    const loginUrl = new URL('/login', request.url);
    return NextResponse.redirect(loginUrl);
  }
}

export const config = {
  matcher: [
    /*
     * Match all paths except:
     * - api routes
     * - _next/static (static files)
     * - _next/image (image optimization)
     * - favicon.ico
     */
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};
