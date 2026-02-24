// Settings types — populated in Phase 6

export interface BarangaySettings {
  id: number;
  barangayName: string;
  municipality: string;
  province: string;
  captainName: string;
  logoMimeType?: string;
  updatedAt: string;
}

export interface FeeConfig {
  id: number;
  standardFee: number;
  rushFee: number;
  updatedAt: string;
}
