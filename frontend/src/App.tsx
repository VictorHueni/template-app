import { useEffect, useState } from "react";

const api = import.meta.env.VITE_API_URL ?? "";

type HelloResponse = {
    message: string;
};

type Theme = "light" | "dark";

export default function App() {
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [name, setName] = useState("world");
    const [theme, setTheme] = useState<Theme>("light");

    const loadGreeting = async (customName?: string) => {
        const effectiveName = customName ?? name;
        setLoading(true);
        setError(null);

        try {
            const url = effectiveName
                ? `${api}/api/hello?name=${encodeURIComponent(effectiveName)}`
                : `${api}/api/hello`;

            const res = await fetch(url);

            if (!res.ok) {
                throw new Error(`Request failed with status ${res.status}`);
            }

            const data = (await res.json()) as HelloResponse;
            setMessage(data.message);
        } catch (e) {
            setMessage(null);
            setError(e instanceof Error ? e.message : String(e));
        } finally {
            setLoading(false);
        }
    };

    // initial load
    useEffect(() => {
        void loadGreeting();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        void loadGreeting(name);
    };

    const toggleTheme = () => {
        setTheme((prev) => (prev === "light" ? "dark" : "light"));
    };

    const clearMessage = () => {
        setMessage(null);
        setError(null);
    };

    return (
        <main
            data-theme={theme}
            style={{
                fontFamily: "system-ui",
                padding: 24,
                maxWidth: 720,
                margin: "8vmin auto",
                backgroundColor: theme === "light" ? "#ffffff" : "#111827",
                color: theme === "light" ? "#111827" : "#f9fafb",
                borderRadius: 12,
                boxShadow:
                    theme === "light"
                        ? "0 10px 25px rgba(15, 23, 42, 0.08)"
                        : "0 10px 25px rgba(15, 23, 42, 0.6)",
            }}
        >
            <header style={{ marginBottom: 16, display: "flex", gap: 12, alignItems: "center" }}>
                <h1 style={{ margin: 0, fontSize: 28 }}>React + TypeScript → Spring</h1>
                <button type="button" onClick={toggleTheme} style={{ marginLeft: "auto" }}>
                    Switch to {theme === "light" ? "dark" : "light"} theme
                </button>
            </header>

            <section style={{ marginBottom: 16 }}>
                <p>API base: {api || "(not set)"}</p>
                {loading && <p aria-label="loading">Loading greeting…</p>}
                {!loading && message && (
                    <p>
                        <strong>Message:</strong>{" "}
                        <span aria-label="greeting-message">{message}</span>
                    </p>
                )}
                {!loading && !message && !error && <p>No greeting loaded yet.</p>}
                {error && (
                    <p style={{ color: "#b91c1c" }} aria-label="error-message">
                        Error: {error}
                    </p>
                )}
            </section>

            <section>
                <form onSubmit={handleSubmit} aria-label="greeting-form">
                    <label htmlFor="name-input">Name</label>
                    <div style={{ display: "flex", gap: 8, marginTop: 4, marginBottom: 8 }}>
                        <input
                            id="name-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="world"
                        />
                        <button type="submit" disabled={loading}>
                            Update greeting
                        </button>
                        <button type="button" onClick={clearMessage}>
                            Clear
                        </button>
                    </div>
                </form>
            </section>
        </main>
    );
}
