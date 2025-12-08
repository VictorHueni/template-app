/**
 * ErrorMessage Component
 *
 * Displays API errors in a user-friendly format.
 * Supports:
 * - General error messages
 * - Field-level validation errors
 * - Retry and dismiss actions
 */

import type { ApiError } from "../../../api/errors";

interface ErrorMessageProps {
    /** The error to display */
    error: ApiError;
    /** Callback when user clicks dismiss */
    onDismiss?: () => void;
    /** Callback when user clicks retry */
    onRetry?: () => void;
}

export function ErrorMessage({ error, onDismiss, onRetry }: ErrorMessageProps) {
    return (
        <div
            role="alert"
            style={{
                padding: 16,
                backgroundColor: "#fef2f2",
                border: "1px solid #fecaca",
                borderRadius: 8,
                color: "#991b1b",
            }}
        >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}>
                <div>
                    <h4 style={{ margin: "0 0 4px 0", fontSize: 16, fontWeight: 600 }}>
                        {error.title}
                    </h4>
                    <p style={{ margin: 0, fontSize: 14 }}>{error.detail}</p>
                </div>
                {onDismiss && (
                    <button
                        type="button"
                        onClick={onDismiss}
                        aria-label="Dismiss"
                        style={{
                            background: "none",
                            border: "none",
                            cursor: "pointer",
                            color: "#991b1b",
                            fontSize: 18,
                            padding: 4,
                        }}
                    >
                        ✕
                    </button>
                )}
            </div>

            {/* Field-level validation errors */}
            {error.fieldErrors && Object.keys(error.fieldErrors).length > 0 && (
                <ul style={{ margin: "12px 0 0 0", paddingLeft: 20, fontSize: 14 }}>
                    {Object.entries(error.fieldErrors).map(([field, message]) => (
                        <li key={field}>
                            <strong>{field}:</strong> {message}
                        </li>
                    ))}
                </ul>
            )}

            {/* Actions */}
            {onRetry && (
                <button
                    type="button"
                    onClick={onRetry}
                    style={{
                        marginTop: 12,
                        padding: "6px 12px",
                        backgroundColor: "#991b1b",
                        color: "white",
                        border: "none",
                        borderRadius: 4,
                        cursor: "pointer",
                        fontSize: 14,
                    }}
                >
                    Retry
                </button>
            )}
        </div>
    );
}
