import { useState, useCallback } from "react";

import type {
    GreetingResponse,
    CreateGreetingRequest,
    UpdateGreetingRequest,
} from "./api/generated";
import { API_BASE_PATH } from "./api/config";
import {
    useGreetings,
    useCreateGreeting,
    useUpdateGreeting,
    useDeleteGreeting,
} from "./features/greetings/hooks";
import {
    GreetingList,
    GreetingForm,
    ErrorMessage,
    LoadingSpinner,
} from "./features/greetings/components";

type Theme = "light" | "dark";

export default function App() {
    const [theme, setTheme] = useState<Theme>("light");
    const [editingGreeting, setEditingGreeting] = useState<GreetingResponse | null>(null);
    const [showCreateForm, setShowCreateForm] = useState(false);

    // Hooks for data fetching and mutations
    const {
        greetings,
        meta,
        loading: listLoading,
        error: listError,
        refresh: refreshList,
        setPage,
    } = useGreetings({ page: 0, size: 5 });

    const {
        mutate: createGreeting,
        loading: createLoading,
        error: createError,
        reset: resetCreate,
    } = useCreateGreeting();

    const {
        mutate: updateGreeting,
        loading: updateLoading,
        error: updateError,
        reset: resetUpdate,
    } = useUpdateGreeting();

    const {
        mutate: deleteGreeting,
        loading: deleteLoading,
        error: deleteError,
    } = useDeleteGreeting();

    // Combined loading and error states
    const isLoading = listLoading || createLoading || updateLoading || deleteLoading;
    const mutationError = createError || updateError || deleteError;

    // Handlers
    const handleCreate = useCallback(
        async (data: CreateGreetingRequest) => {
            const result = await createGreeting(data);
            if (result) {
                setShowCreateForm(false);
                resetCreate();
                await refreshList();
            }
        },
        [createGreeting, resetCreate, refreshList],
    );

    const handleUpdate = useCallback(
        async (data: CreateGreetingRequest) => {
            if (!editingGreeting) return;
            // Convert CreateGreetingRequest to UpdateGreetingRequest (recipient is required)
            const updateData: UpdateGreetingRequest = {
                message: data.message,
                recipient: data.recipient ?? "", // Default to empty string if not provided
            };
            const result = await updateGreeting(editingGreeting.id, updateData);
            if (result) {
                setEditingGreeting(null);
                resetUpdate();
                await refreshList();
            }
        },
        [editingGreeting, updateGreeting, resetUpdate, refreshList],
    );

    const handleDelete = useCallback(
        async (id: string) => {
            if (window.confirm("Are you sure you want to delete this greeting?")) {
                const success = await deleteGreeting(id);
                if (success) {
                    await refreshList();
                }
            }
        },
        [deleteGreeting, refreshList],
    );

    const handleEdit = useCallback((greeting: GreetingResponse) => {
        setEditingGreeting(greeting);
        setShowCreateForm(false);
    }, []);

    const handleCancelEdit = useCallback(() => {
        setEditingGreeting(null);
        resetUpdate();
    }, [resetUpdate]);

    const handleCancelCreate = useCallback(() => {
        setShowCreateForm(false);
        resetCreate();
    }, [resetCreate]);

    const handlePageChange = useCallback(
        (page: number) => {
            setPage(page);
        },
        [setPage],
    );

    const toggleTheme = () => {
        setTheme((prev) => (prev === "light" ? "dark" : "light"));
    };

    const isDark = theme === "dark";

    return (
        <main
            data-theme={theme}
            style={{
                fontFamily: "system-ui",
                padding: 24,
                maxWidth: 800,
                margin: "4vmin auto",
                backgroundColor: isDark ? "#111827" : "#ffffff",
                color: isDark ? "#f9fafb" : "#111827",
                borderRadius: 12,
                boxShadow: isDark
                    ? "0 10px 25px rgba(15, 23, 42, 0.6)"
                    : "0 10px 25px rgba(15, 23, 42, 0.08)",
            }}
        >
            {/* Header */}
            <header style={{ marginBottom: 24, display: "flex", gap: 12, alignItems: "center" }}>
                <h1 style={{ margin: 0, fontSize: 28 }}>Greetings App</h1>
                <button type="button" onClick={toggleTheme} style={{ marginLeft: "auto" }}>
                    {isDark ? "☀️ Light" : "🌙 Dark"}
                </button>
            </header>

            {/* API Info */}
            <p style={{ fontSize: 14, color: isDark ? "#9ca3af" : "#6b7280", marginBottom: 16 }}>
                API: {API_BASE_PATH}
            </p>

            {/* Global Error Display */}
            {mutationError && (
                <ErrorMessage
                    error={mutationError}
                    onDismiss={() => {
                        resetCreate();
                        resetUpdate();
                    }}
                />
            )}

            {/* Create/Edit Form Section */}
            <section style={{ marginBottom: 24 }}>
                {!showCreateForm && !editingGreeting && (
                    <button
                        type="button"
                        onClick={() => setShowCreateForm(true)}
                        style={{
                            padding: "10px 20px",
                            backgroundColor: "#2563eb",
                            color: "white",
                            border: "none",
                            borderRadius: 6,
                            cursor: "pointer",
                            fontSize: 16,
                        }}
                    >
                        + New Greeting
                    </button>
                )}

                {showCreateForm && (
                    <div
                        style={{
                            padding: 16,
                            backgroundColor: isDark ? "#1f2937" : "#f3f4f6",
                            borderRadius: 8,
                        }}
                    >
                        <h2 style={{ margin: "0 0 12px 0", fontSize: 18 }}>Create New Greeting</h2>
                        <GreetingForm
                            onSubmit={handleCreate}
                            onCancel={handleCancelCreate}
                            loading={createLoading}
                            fieldErrors={createError?.fieldErrors}
                        />
                    </div>
                )}

                {editingGreeting && (
                    <div
                        style={{
                            padding: 16,
                            backgroundColor: isDark ? "#1f2937" : "#f3f4f6",
                            borderRadius: 8,
                        }}
                    >
                        <h2 style={{ margin: "0 0 12px 0", fontSize: 18 }}>Edit Greeting</h2>
                        <GreetingForm
                            onSubmit={handleUpdate}
                            onCancel={handleCancelEdit}
                            initialValues={{
                                message: editingGreeting.message,
                                recipient: editingGreeting.recipient,
                            }}
                            loading={updateLoading}
                            fieldErrors={updateError?.fieldErrors}
                            submitLabel="Update"
                        />
                    </div>
                )}
            </section>

            {/* Greetings List Section */}
            <section>
                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        marginBottom: 16,
                        gap: 12,
                    }}
                >
                    <h2 style={{ margin: 0, fontSize: 20 }}>All Greetings</h2>
                    <button
                        type="button"
                        onClick={() => void refreshList()}
                        disabled={isLoading}
                        style={{
                            padding: "6px 12px",
                            backgroundColor: "transparent",
                            border: `1px solid ${isDark ? "#4b5563" : "#d1d5db"}`,
                            borderRadius: 4,
                            cursor: isLoading ? "not-allowed" : "pointer",
                            color: isDark ? "#f9fafb" : "#111827",
                        }}
                    >
                        🔄 Refresh
                    </button>
                </div>

                {listLoading && !greetings.length && (
                    <LoadingSpinner message="Loading greetings..." />
                )}

                {listError && !listLoading && (
                    <ErrorMessage error={listError} onRetry={() => void refreshList()} />
                )}

                {!listError && (
                    <GreetingList
                        greetings={greetings}
                        meta={meta}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                        onPageChange={handlePageChange}
                    />
                )}
            </section>
        </main>
    );
}
