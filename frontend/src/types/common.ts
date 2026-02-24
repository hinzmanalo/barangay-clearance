// Shared / common types

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  details?: Record<string, string>;
}
