import type { ApiError } from "../../api/errors";
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

export interface AuthProviderProps {
    children: React.ReactNode;
    mode?: AuthMode;
    mockUser?: UserInfoResponse;
}
