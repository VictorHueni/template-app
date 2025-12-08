/**
 * GreetingList Component
 *
 * Displays a paginated list of greetings with navigation controls.
 */

import type { GreetingResponse, PageMeta } from "../../../api/generated";
import { GreetingCard } from "./GreetingCard";

interface GreetingListProps {
    /** Array of greetings to display */
    greetings: GreetingResponse[];
    /** Pagination metadata */
    meta?: PageMeta | null;
    /** Callback when user changes page */
    onPageChange?: (page: number) => void;
    /** Callback when user clicks edit on a greeting */
    onEdit?: (greeting: GreetingResponse) => void;
    /** Callback when user clicks delete on a greeting */
    onDelete?: (id: number) => void;
}

export function GreetingList({
    greetings,
    meta,
    onPageChange,
    onEdit,
    onDelete,
}: GreetingListProps) {
    // Empty state
    if (greetings.length === 0) {
        return (
            <div
                style={{
                    padding: 32,
                    textAlign: "center",
                    color: "#6b7280",
                    backgroundColor: "#f9fafb",
                    borderRadius: 8,
                }}
            >
                <p style={{ margin: 0, fontSize: 16 }}>No greetings found.</p>
                <p style={{ margin: "8px 0 0 0", fontSize: 14 }}>
                    Create your first greeting to get started!
                </p>
            </div>
        );
    }

    return (
        <div>
            {/* Greeting cards */}
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                {greetings.map((greeting) => (
                    <GreetingCard
                        key={greeting.id}
                        greeting={greeting}
                        onEdit={onEdit}
                        onDelete={onDelete}
                    />
                ))}
            </div>

            {/* Pagination */}
            {meta && (
                <nav
                    aria-label="Pagination"
                    style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginTop: 16,
                        padding: "12px 0",
                        borderTop: "1px solid #e5e7eb",
                    }}
                >
                    <span style={{ fontSize: 14, color: "#6b7280" }}>
                        Page {meta.pageNumber + 1} of {meta.totalPages} ({meta.totalElements} total)
                    </span>

                    {onPageChange && (
                        <div style={{ display: "flex", gap: 8 }}>
                            <button
                                type="button"
                                onClick={() => onPageChange(meta.pageNumber - 1)}
                                disabled={meta.pageNumber === 0}
                                style={{
                                    padding: "6px 12px",
                                    backgroundColor: meta.pageNumber === 0 ? "#e5e7eb" : "#3b82f6",
                                    color: meta.pageNumber === 0 ? "#9ca3af" : "white",
                                    border: "none",
                                    borderRadius: 4,
                                    cursor: meta.pageNumber === 0 ? "not-allowed" : "pointer",
                                    fontSize: 14,
                                }}
                            >
                                Previous
                            </button>
                            <button
                                type="button"
                                onClick={() => onPageChange(meta.pageNumber + 1)}
                                disabled={meta.pageNumber >= meta.totalPages - 1}
                                style={{
                                    padding: "6px 12px",
                                    backgroundColor:
                                        meta.pageNumber >= meta.totalPages - 1
                                            ? "#e5e7eb"
                                            : "#3b82f6",
                                    color:
                                        meta.pageNumber >= meta.totalPages - 1
                                            ? "#9ca3af"
                                            : "white",
                                    border: "none",
                                    borderRadius: 4,
                                    cursor:
                                        meta.pageNumber >= meta.totalPages - 1
                                            ? "not-allowed"
                                            : "pointer",
                                    fontSize: 14,
                                }}
                            >
                                Next
                            </button>
                        </div>
                    )}
                </nav>
            )}
        </div>
    );
}
