// Settings types — Phase 6

export interface BarangaySettings {
  id: number;
  barangayName: string;
  municipality: string;
  province: string;
  captainName: string;
  /** true if a logo image has been uploaded */
  hasLogo: boolean;
  logoMimeType?: string;
  updatedAt: string;
}

export interface FeeConfig {
  id: number;
  standardFee: number;
  rushFee: number;
  updatedAt: string;
}

export interface UpdateBarangaySettingsPayload {
  barangayName: string;
  municipality: string;
  province: string;
  captainName: string;
}

export interface UpdateFeeConfigPayload {
  standardFee: number;
  rushFee: number;
}
