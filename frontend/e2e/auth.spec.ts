import { test, expect } from "@playwright/test";

test.beforeEach(async ({ page }) => {
    // Keep greetings deterministic too (App always loads the list).
    await page.route("**/api/v1/greetings**", (route) => {
        if (route.request().method() === "GET") {
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    data: [],
                    meta: { pageNumber: 0, pageSize: 5, totalElements: 0, totalPages: 0 },
                }),
            });
        } else {
            route.fulfill({ status: 401 });
        }
    });
});

test("shows anonymous state when /me is 401", async ({ page }) => {
    await page.route("**/api/v1/me", (route) => {
        route.fulfill({
            status: 401,
            contentType: "application/problem+json",
            body: JSON.stringify({
                type: "https://api.example.com/problems/unauthorized",
                title: "Unauthorized",
                status: 401,
                detail: "Authentication is required",
                timestamp: "2025-01-15T10:30:00Z",
                traceId: "550e8400-e29b-41d4-a716-446655440000",
            }),
        });
    });

    await page.goto("/");

    await expect(page.getByText(/not signed in/i)).toBeVisible();
    await expect(page.getByRole("button", { name: /sign in/i })).toBeVisible();
});

test("shows authenticated state when /me returns user", async ({ page }) => {
    await page.route("**/api/v1/me", (route) => {
        route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
                id: "u-1",
                username: "johndoe",
                roles: ["USER"],
                email: "john.doe@example.com",
            }),
        });
    });

    await page.goto("/");

    await expect(page.getByText(/signed in as johndoe/i)).toBeVisible();
    await expect(page.getByRole("button", { name: /sign out/i })).toBeVisible();
});
