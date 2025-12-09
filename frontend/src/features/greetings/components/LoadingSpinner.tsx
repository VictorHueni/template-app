/**
 * LoadingSpinner Component
 *
 * A reusable loading indicator with optional message.
 * Includes accessibility features (role, aria-label).
 */

interface LoadingSpinnerProps {
    /** Optional message to display below the spinner */
    message?: string;
    /** Size of the spinner: small, medium, large */
    size?: "small" | "medium" | "large";
}

const sizeMap = {
    small: 16,
    medium: 24,
    large: 40,
};

export function LoadingSpinner({ message = "Loading...", size = "medium" }: LoadingSpinnerProps) {
    const dimension = sizeMap[size];

    return (
        <div
            role="status"
            aria-label="Loading"
            style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 8,
                padding: 16,
            }}
        >
            {/* CSS-based spinner - no external dependencies */}
            <div
                style={{
                    width: dimension,
                    height: dimension,
                    border: `3px solid #e5e7eb`,
                    borderTopColor: "#3b82f6",
                    borderRadius: "50%",
                    animation: "spin 1s linear infinite",
                }}
            />
            {message && <p style={{ margin: 0, color: "#6b7280", fontSize: 14 }}>{message}</p>}
            {/* Inline keyframes for the spinner animation */}
            <style>{`
                @keyframes spin {
                    to { transform: rotate(360deg); }
                }
            `}</style>
        </div>
    );
}
