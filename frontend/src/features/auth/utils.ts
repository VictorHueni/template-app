import type { AuthMode } from "./types";

export function resolveAuthMode(mode?: AuthMode): AuthMode {
    const envMode = (import.meta.env.VITE_AUTH_MODE as AuthMode | undefined) ?? "real";
    const requested = mode ?? envMode;
    if (import.meta.env.PROD && requested === "mock") {
        return "real";
    }
    return requested;
}

export async function resolveLoginUri(): Promise<string> {
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
