/**
 * GreetingCard Component
 *
 * Displays a single greeting with optional edit/delete actions.
 * Used in the greeting list and can be used standalone for detail views.
 */

import type { GreetingResponse } from "../../../api/generated";

interface GreetingCardProps {
    /** The greeting to display */
    greeting: GreetingResponse;
    /** Callback when edit button is clicked */
    onEdit?: (greeting: GreetingResponse) => void;
    /** Callback when delete button is clicked */
    onDelete?: (id: number) => void;
}

export function GreetingCard({ greeting, onEdit, onDelete }: GreetingCardProps) {
    const formattedDate = greeting.createdAt
        ? new Intl.DateTimeFormat("en-US", {
              dateStyle: "medium",
              timeStyle: "short",
          }).format(new Date(greeting.createdAt))
        : null;

    return (
        <article
            style={{
                padding: 16,
                border: "1px solid #e5e7eb",
                borderRadius: 8,
                backgroundColor: "#ffffff",
            }}
        >
            {/* Header with reference */}
            <header
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: 8,
                }}
            >
                <span
                    style={{
                        fontSize: 12,
                        color: "#6b7280",
                        fontFamily: "monospace",
                        backgroundColor: "#f3f4f6",
                        padding: "2px 6px",
                        borderRadius: 4,
                    }}
                >
                    {greeting.reference}
                </span>
                {formattedDate && (
                    <time style={{ fontSize: 12, color: "#9ca3af" }}>{formattedDate}</time>
                )}
            </header>

            {/* Message content */}
            <p style={{ margin: "0 0 8px 0", fontSize: 16, lineHeight: 1.5 }}>{greeting.message}</p>

            {/* Recipient */}
            {greeting.recipient && (
                <p style={{ margin: "0 0 12px 0", fontSize: 14, color: "#6b7280" }}>
                    <strong>To:</strong> {greeting.recipient}
                </p>
            )}

            {/* Action buttons */}
            {(onEdit || onDelete) && (
                <footer
                    style={{
                        display: "flex",
                        gap: 8,
                        paddingTop: 12,
                        borderTop: "1px solid #f3f4f6",
                    }}
                >
                    {onEdit && (
                        <button
                            type="button"
                            onClick={() => onEdit(greeting)}
                            style={{
                                padding: "6px 12px",
                                backgroundColor: "#3b82f6",
                                color: "white",
                                border: "none",
                                borderRadius: 4,
                                cursor: "pointer",
                                fontSize: 14,
                            }}
                        >
                            Edit
                        </button>
                    )}
                    {onDelete && (
                        <button
                            type="button"
                            onClick={() => onDelete(greeting.id)}
                            style={{
                                padding: "6px 12px",
                                backgroundColor: "#ef4444",
                                color: "white",
                                border: "none",
                                borderRadius: 4,
                                cursor: "pointer",
                                fontSize: 14,
                            }}
                        >
                            Delete
                        </button>
                    )}
                </footer>
            )}
        </article>
    );
}
