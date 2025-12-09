/**
 * GreetingForm Component
 *
 * Form for creating or editing a greeting.
 * Features:
 * - Client-side validation
 * - Server-side error display
 * - Loading state handling
 */

import { useState, type FormEvent } from "react";
import type { CreateGreetingRequest } from "../../../api/generated";

interface GreetingFormProps {
    /** Callback when form is submitted with valid data */
    onSubmit: (data: CreateGreetingRequest) => void;
    /** Initial form values (for editing) */
    initialValues?: Partial<CreateGreetingRequest>;
    /** Field-level errors from API validation */
    fieldErrors?: Record<string, string>;
    /** Whether the form is submitting */
    loading?: boolean;
    /** Cancel button callback (optional) */
    onCancel?: () => void;
    /** Submit button label */
    submitLabel?: string;
}

export function GreetingForm({
    onSubmit,
    initialValues = {},
    fieldErrors = {},
    loading = false,
    onCancel,
    submitLabel = "Create Greeting",
}: GreetingFormProps) {
    const [message, setMessage] = useState(initialValues.message ?? "");
    const [recipient, setRecipient] = useState(initialValues.recipient ?? "");
    const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

    // Combine local validation errors with server errors
    const errors = { ...localErrors, ...fieldErrors };

    const validate = (): boolean => {
        const newErrors: Record<string, string> = {};

        if (!message.trim()) {
            newErrors.message = "Message is required";
        }

        setLocalErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();

        if (!validate()) {
            return;
        }

        onSubmit({
            message: message.trim(),
            recipient: recipient.trim() || undefined,
        });
    };

    return (
        <form onSubmit={handleSubmit}>
            {/* Message field */}
            <div style={{ marginBottom: 16 }}>
                <label
                    htmlFor="greeting-message"
                    style={{
                        display: "block",
                        marginBottom: 4,
                        fontWeight: 500,
                        fontSize: 14,
                    }}
                >
                    Message *
                </label>
                <textarea
                    id="greeting-message"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="Enter your greeting message..."
                    disabled={loading}
                    rows={3}
                    style={{
                        width: "100%",
                        padding: "8px 12px",
                        borderRadius: 4,
                        border: errors.message ? "1px solid #ef4444" : "1px solid #d1d5db",
                        fontSize: 14,
                        resize: "vertical",
                        boxSizing: "border-box",
                    }}
                />
                {errors.message && (
                    <p style={{ margin: "4px 0 0 0", color: "#ef4444", fontSize: 12 }}>
                        {errors.message}
                    </p>
                )}
            </div>

            {/* Recipient field */}
            <div style={{ marginBottom: 16 }}>
                <label
                    htmlFor="greeting-recipient"
                    style={{
                        display: "block",
                        marginBottom: 4,
                        fontWeight: 500,
                        fontSize: 14,
                    }}
                >
                    Recipient (optional)
                </label>
                <input
                    id="greeting-recipient"
                    type="text"
                    value={recipient}
                    onChange={(e) => setRecipient(e.target.value)}
                    placeholder="Who is this greeting for?"
                    disabled={loading}
                    style={{
                        width: "100%",
                        padding: "8px 12px",
                        borderRadius: 4,
                        border: errors.recipient ? "1px solid #ef4444" : "1px solid #d1d5db",
                        fontSize: 14,
                        boxSizing: "border-box",
                    }}
                />
                {errors.recipient && (
                    <p style={{ margin: "4px 0 0 0", color: "#ef4444", fontSize: 12 }}>
                        {errors.recipient}
                    </p>
                )}
            </div>

            {/* Actions */}
            <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
                {onCancel && (
                    <button
                        type="button"
                        onClick={onCancel}
                        disabled={loading}
                        style={{
                            padding: "8px 16px",
                            backgroundColor: "#f3f4f6",
                            color: "#374151",
                            border: "1px solid #d1d5db",
                            borderRadius: 4,
                            cursor: loading ? "not-allowed" : "pointer",
                            fontSize: 14,
                        }}
                    >
                        Cancel
                    </button>
                )}
                <button
                    type="submit"
                    disabled={loading}
                    style={{
                        padding: "8px 16px",
                        backgroundColor: loading ? "#9ca3af" : "#3b82f6",
                        color: "white",
                        border: "none",
                        borderRadius: 4,
                        cursor: loading ? "not-allowed" : "pointer",
                        fontSize: 14,
                    }}
                >
                    {loading ? "Saving..." : submitLabel}
                </button>
            </div>
        </form>
    );
}
