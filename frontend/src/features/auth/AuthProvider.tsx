import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

import { getCurrentUser } from "../../api/config";
import { parseApiError, isUnauthorizedError, type ApiError } from "../../api/errors";
import type { UserInfoResponse } from "../../api/generated";

export type AuthStatus = "loading" | "authenticated" | "anonymous";
export type AuthMode = "real" | "mock";

export interface AuthContextValue {
    status: AuthStatus;
    user: UserInfoResponse | null;
    error: ApiError | null;
    refresh: () => Promise<void>;
    login: () => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function resolveAuthMode(mode?: AuthMode): AuthMode {
    const envMode = (import.meta.env.VITE_AUTH_MODE as AuthMode | undefined) ?? "real";
    return mode ?? envMode;
}

async function resolveLoginUri(): Promise<string> {
    try {
        const res = await fetch("/login-options", { credentials: "include" });
        if (res.ok) {
            const body = (await res.json()) as { loginUri?: string };
            if (body.loginUri) {
                return body.loginUri;
            }
        }
    } catch {
        // ignore - fallback below
    }

    return "/oauth2/authorization/keycloak";
}

export interface AuthProviderProps {
    children: React.ReactNode;
    mode?: AuthMode;
    mockUser?: UserInfoResponse;
}

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

        const { data, error: responseError } = await getCurrentUser();

        if (responseError) {
            const apiError = await parseApiError(responseError);
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

        if (data) {
            setUser(data);
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

        window.location.assign("/logout");
    }, [resolvedMode]);

    const value: AuthContextValue = useMemo(
        () => ({ status, user, error, refresh, login, logout }),
        [status, user, error, refresh, login, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        throw new Error("useAuth must be used within AuthProvider");
    }
    return ctx;
}
