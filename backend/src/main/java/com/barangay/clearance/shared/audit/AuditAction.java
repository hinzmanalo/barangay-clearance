package com.barangay.clearance.shared.audit;

/**
 * Audit action string constants used when writing {@link AuditLog} records.
 *
 * <p>
 * Using a constants class (rather than an enum) keeps the set open for
 * extension by future modules without modifying a shared enum.
 * </p>
 *
 * <p>
 * Each constant maps to the {@code action} column of the {@code audit_logs}
 * table and must not exceed 100 characters.
 * </p>
 */
public final class AuditAction {

    private AuditAction() {
        // Non-instantiable constants class
    }

    // ── Identity: Auth ───────────────────────────────────────────────────────

    /** A new resident account was registered via the portal. */
    public static final String USER_REGISTERED = "USER_REGISTERED";

    /** A user successfully authenticated and received tokens. */
    public static final String USER_LOGIN = "USER_LOGIN";

    /** A login attempt was made but failed (wrong password or user not found). */
    public static final String USER_LOGIN_FAILED = "USER_LOGIN_FAILED";

    /** A user's refresh token was revoked (logout). */
    public static final String USER_LOGOUT = "USER_LOGOUT";

    /** A new access token was issued via a valid refresh token. */
    public static final String USER_TOKEN_REFRESHED = "USER_TOKEN_REFRESHED";

    /** A user changed their own password (auth flow). */
    public static final String USER_PASSWORD_CHANGED = "USER_PASSWORD_CHANGED";

    // ── Identity: Staff management ───────────────────────────────────────────

    /** An admin created a new staff account. */
    public static final String STAFF_CREATED = "STAFF_CREATED";

    /** An admin reactivated a deactivated staff account. */
    public static final String STAFF_ACTIVATED = "STAFF_ACTIVATED";

    /** An admin deactivated a staff account. */
    public static final String STAFF_DEACTIVATED = "STAFF_DEACTIVATED";

    /** An admin changed the role of a staff account. */
    public static final String STAFF_ROLE_CHANGED = "STAFF_ROLE_CHANGED";

    /** An admin force-reset a staff user's password. */
    public static final String STAFF_PASSWORD_RESET = "STAFF_PASSWORD_RESET";

    // ── Residents ────────────────────────────────────────────────────────────

    /** A resident profile was created (walk-in or via registration). */
    public static final String RESIDENT_CREATED = "RESIDENT_CREATED";

    /** An existing resident profile was updated. */
    public static final String RESIDENT_UPDATED = "RESIDENT_UPDATED";

    /** A pending resident's portal account was activated by staff. */
    public static final String RESIDENT_ACTIVATED = "RESIDENT_ACTIVATED";

    // ── Clearance ────────────────────────────────────────────────────────────

    /** A clearance request was submitted (portal or walk-in). */
    public static final String CLEARANCE_SUBMITTED = "CLEARANCE_SUBMITTED";

    /** A clearance request was approved by staff. */
    public static final String CLEARANCE_APPROVED = "CLEARANCE_APPROVED";

    /** A clearance request was rejected by staff. */
    public static final String CLEARANCE_REJECTED = "CLEARANCE_REJECTED";

    /** A resident resubmitted a previously rejected request. */
    public static final String CLEARANCE_RESUBMITTED = "CLEARANCE_RESUBMITTED";

    /** An approved and paid clearance was released by staff. */
    public static final String CLEARANCE_RELEASED = "CLEARANCE_RELEASED";

    /** A released clearance PDF was downloaded. */
    public static final String CLEARANCE_PDF_DOWNLOADED = "CLEARANCE_PDF_DOWNLOADED";

    // ── Payments ─────────────────────────────────────────────────────────────

    /** A payment was initiated via the payment gateway. */
    public static final String PAYMENT_INITIATED = "PAYMENT_INITIATED";

    /** The payment gateway returned a successful result. */
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";

    /** The payment gateway returned a failure result. */
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    /** A cash payment was manually recorded by a clerk. */
    public static final String PAYMENT_CASH_RECORDED = "PAYMENT_CASH_RECORDED";

    // ── Settings ─────────────────────────────────────────────────────────────

    /** Barangay settings (name, captain, etc.) were updated. */
    public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";

    /** The barangay logo image was uploaded or replaced. */
    public static final String SETTINGS_LOGO_UPLOADED = "SETTINGS_LOGO_UPLOADED";

    /** Standard and/or rush fee amounts were updated. */
    public static final String FEES_UPDATED = "FEES_UPDATED";
}
