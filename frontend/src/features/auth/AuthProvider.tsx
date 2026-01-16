import { useCallback, useEffect, useMemo, useState } from "react";

import { getCurrentUser } from "../../api/config";
import { parseApiError, isUnauthorizedError, type ApiError } from "../../api/errors";
import type { UserInfoResponse } from "../../api/generated";
import { AuthContext } from "./hooks";
import type { AuthContextValue, AuthProviderProps, AuthStatus } from "./types";
import { resolveAuthMode, resolveLoginUri } from "./utils";

export function AuthProvider({ children, mode, mockUser }: AuthProviderProps) {
    const resolvedMode = resolveAuthMode(mode);

    const [status, setStatus] = useState<AuthStatus>("loading");
    const [user, setUser] = useState<UserInfoResponse | null>(null);
    const [error, setError] = useState<ApiError | null>(null);

    const load = useCallback(async () => {
        if (resolvedMode === "mock") {
            setUser(
                mockUser ?? {
                    id: "mock-user",
                    username: "mockuser",
                    roles: ["USER"],
                },
            );
            setStatus("authenticated");
            setError(null);
            return;
        }

        setStatus("loading");
        setError(null);

        const result = await getCurrentUser();

        if (result.error !== undefined) {
            // hey-api returns empty string when gateway returns no body (e.g., 401)
            // In that case, pass the Response object to extract the status code
            const errorToParse =
                result.error === ("" as unknown) && result.response && !result.response.ok
                    ? result.response
                    : result.error;
            const apiError = await parseApiError(errorToParse);
            if (isUnauthorizedError(apiError)) {
                setUser(null);
                setStatus("anonymous");
                setError(null);
                return;
            }

            setUser(null);
            setStatus("anonymous");
            setError(apiError);
            return;
        }

        if (result.data) {
            setUser(result.data);
            setStatus("authenticated");
            setError(null);
            return;
        }

        setUser(null);
        setStatus("anonymous");
        setError(null);
    }, [mockUser, resolvedMode]);

    useEffect(() => {
        void load();
    }, [load]);

    useEffect(() => {
        if (resolvedMode === "mock" || typeof window === "undefined") {
            return;
        }

        const onSessionExpired = () => {
            setUser(null);
            setStatus("anonymous");
            setError(null);
        };

        window.addEventListener("auth:session-expired", onSessionExpired);
        return () => {
            window.removeEventListener("auth:session-expired", onSessionExpired);
        };
    }, [resolvedMode]);

    const refresh = useCallback(async () => {
        await load();
    }, [load]);

    const login = useCallback(async () => {
        if (resolvedMode === "mock") {
            setStatus("authenticated");
            if (!user) {
                setUser(
                    mockUser ?? {
                        id: "mock-user",
                        username: "mockuser",
                        roles: ["USER"],
                    },
                );
            }
            return;
        }

        const loginUri = await resolveLoginUri();
        window.location.assign(loginUri);
    }, [mockUser, resolvedMode, user]);

    const logout = useCallback(async () => {
        if (resolvedMode === "mock") {
            setUser(null);
            setStatus("anonymous");
            setError(null);
            return;
        }

        try {
            // Extract CSRF token from cookie (same pattern as api/config.ts)
            const csrfToken = document.cookie
                .split(";")
                .map((c) => c.trim())
                .find((c) => c.startsWith("XSRF-TOKEN="))
                ?.substring("XSRF-TOKEN=".length);

            const headers: Record<string, string> = {
                "X-POST-LOGOUT-SUCCESS-URI": window.location.origin,
            };

            if (csrfToken) {
                headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfToken);
            }

            const response = await fetch("/logout", {
                method: "POST",
                credentials: "include",
                headers,
            });

            const logoutUri = response.headers.get("Location");
            if (logoutUri) {
                window.location.href = logoutUri;
            } else {
                window.location.reload();
            }
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error("Logout failed:", error);
            window.location.href = "/";
        }
    }, [resolvedMode]);

    const value: AuthContextValue = useMemo(
        () => ({ status, user, error, refresh, login, logout }),
        [status, user, error, refresh, login, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
