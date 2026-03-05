export interface AuditLog {
  id: string;
  userId: string | null;
  actorEmail: string | null;
  action: string;
  entityType: string | null;
  entityId: string | null;
  details: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AuditLogFilters {
  action?: string;
  entityType?: string;
  userId?: string;
  from?: string;
  to?: string;
}
