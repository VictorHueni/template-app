import { test, expect, type Page } from "@playwright/test";

/**
 * E2E Tests for Greetings App
 *
 * These tests verify the complete user flows of the application:
 * - Viewing greetings list
 * - Creating new greetings
 * - Editing existing greetings
 * - Deleting greetings
 * - Theme toggling
 *
 * STRATEGY:
 * We use Playwright's route interception to mock API responses.
 * This makes tests fast, deterministic, and independent of backend.
 */

// Mock data for tests
const mockGreetings = [
    {
        id: 1001,
        reference: "GRE-2025-000001",
        message: "Hello, World!",
        recipient: "World",
        createdAt: "2025-01-15T10:30:00Z",
    },
    {
        id: 1002,
        reference: "GRE-2025-000002",
        message: "Welcome to the app!",
        recipient: "User",
        createdAt: "2025-01-15T11:00:00Z",
    },
    {
        id: 1003,
        reference: "GRE-2025-000003",
        message: "Greetings from E2E!",
        recipient: "Tester",
        createdAt: "2025-01-15T11:30:00Z",
    },
];

const mockGreetingPage = {
    data: mockGreetings,
    meta: {
        pageNumber: 0,
        pageSize: 5,
        totalElements: 3,
        totalPages: 1,
    },
};

const emptyGreetingPage = {
    data: [],
    meta: {
        pageNumber: 0,
        pageSize: 5,
        totalElements: 0,
        totalPages: 0,
    },
};

/**
 * Helper to set up API mocking for the greetings list
 */
async function mockGreetingsApi(page: Page, response = mockGreetingPage) {
    await page.route("**/api/v1/greetings**", (route) => {
        if (route.request().method() === "GET") {
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(response),
            });
        } else {
            route.continue();
        }
    });
}

/**
 * Helper to mock all CRUD operations
 */
async function mockAllGreetingsApis(page: Page) {
    await page.route("**/api/v1/greetings**", (route) => {
        const method = route.request().method();
        const url = route.request().url();

        if (method === "GET") {
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(mockGreetingPage),
            });
        } else if (method === "POST") {
            const newGreeting = {
                id: 9999,
                reference: "GRE-2025-009999",
                message: "New E2E greeting",
                recipient: "E2E Tester",
                createdAt: new Date().toISOString(),
            };
            route.fulfill({
                status: 201,
                contentType: "application/json",
                body: JSON.stringify(newGreeting),
            });
        } else if (method === "PUT" || method === "PATCH") {
            // Extract ID from URL
            const idMatch = url.match(/\/greetings\/(\d+)/);
            const id = idMatch ? parseInt(idMatch[1]) : 1001;
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    ...mockGreetings[0],
                    id,
                    message: "Updated message",
                }),
            });
        } else if (method === "DELETE") {
            route.fulfill({ status: 204 });
        } else {
            route.continue();
        }
    });
}

// ============================================================
// Basic App Loading Tests
// ============================================================

test.describe("App Loading", () => {
    test("displays app title and API info", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        // Title visible
        await expect(page.getByRole("heading", { name: /greetings app/i })).toBeVisible();

        // API base text rendered
        await expect(page.getByText(/API:/i)).toBeVisible();
        await expect(page.getByText(/\/api\/v1/i)).toBeVisible();
    });

    test("shows loading state initially", async ({ page }) => {
        // Delay the API response to see loading state
        await page.route("**/api/v1/greetings**", async (route) => {
            await new Promise((resolve) => setTimeout(resolve, 500));
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(mockGreetingPage),
            });
        });

        await page.goto("/");

        // Loading indicator should be visible initially
        await expect(page.getByRole("status", { name: /loading/i })).toBeVisible();

        // After loading, greetings should appear
        await expect(page.getByText("Hello, World!")).toBeVisible({ timeout: 10000 });
    });
});

// ============================================================
// Theme Toggle Tests
// ============================================================

test.describe("Theme Toggle", () => {
    test("toggles between light and dark theme", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        const main = page.locator("main");

        // Starts in light mode
        await expect(main).toHaveAttribute("data-theme", "light");

        // Click dark mode button
        const darkButton = page.getByRole("button", { name: /dark/i });
        await darkButton.click();

        // Now in dark mode
        await expect(main).toHaveAttribute("data-theme", "dark");

        // Light button should now be visible
        const lightButton = page.getByRole("button", { name: /light/i });
        await expect(lightButton).toBeVisible();

        // Toggle back
        await lightButton.click();
        await expect(main).toHaveAttribute("data-theme", "light");
    });
});

// ============================================================
// Greetings List Display Tests
// ============================================================

test.describe("Greetings List", () => {
    test("displays list of greetings", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        // Wait for greetings to load
        await expect(page.getByText("Hello, World!")).toBeVisible();
        await expect(page.getByText("Welcome to the app!")).toBeVisible();
        await expect(page.getByText("Greetings from E2E!")).toBeVisible();

        // Recipients visible (use specific "To:" prefix to avoid ambiguity)
        await expect(page.getByText("To: World")).toBeVisible();
        await expect(page.getByText("To: User")).toBeVisible();
        await expect(page.getByText("To: Tester")).toBeVisible();
    });

    test("shows empty state when no greetings", async ({ page }) => {
        await mockGreetingsApi(page, emptyGreetingPage);
        await page.goto("/");

        await expect(page.getByText(/no greetings found/i)).toBeVisible();
        await expect(page.getByText(/create your first greeting/i)).toBeVisible();
    });

    test("shows error state on API failure", async ({ page }) => {
        await page.route("**/api/v1/greetings**", (route) => {
            route.fulfill({
                status: 500,
                contentType: "application/json",
                body: JSON.stringify({
                    type: "about:blank",
                    title: "Internal Server Error",
                    status: 500,
                    detail: "Database connection failed",
                }),
            });
        });

        await page.goto("/");

        // Error alert should be visible
        await expect(page.getByRole("alert")).toBeVisible();
    });

    test("refreshes list when clicking refresh button", async ({ page }) => {
        let callCount = 0;
        await page.route("**/api/v1/greetings**", (route) => {
            callCount++;
            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify(mockGreetingPage),
            });
        });

        await page.goto("/");
        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Record initial call count (may be 1 or 2 depending on React strict mode)
        const initialCallCount = callCount;
        expect(initialCallCount).toBeGreaterThanOrEqual(1);

        // Click refresh
        await page.getByRole("button", { name: /refresh/i }).click();

        // Wait for the refresh call
        await page.waitForTimeout(500);

        // Should have made at least one more call
        expect(callCount).toBeGreaterThan(initialCallCount);
    });
});

// ============================================================
// Create Greeting Tests
// ============================================================

test.describe("Create Greeting", () => {
    test("opens create form and can cancel", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        // Wait for page to load
        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Click new greeting button
        await page.getByRole("button", { name: /new greeting/i }).click();

        // Form should be visible
        await expect(page.getByRole("heading", { name: /create new greeting/i })).toBeVisible();
        await expect(page.getByLabel(/message/i)).toBeVisible();
        await expect(page.getByLabel(/recipient/i)).toBeVisible();

        // Cancel button closes form
        await page.getByRole("button", { name: /cancel/i }).click();

        // Form should be hidden
        await expect(page.getByRole("heading", { name: /create new greeting/i })).not.toBeVisible();
    });

    test("validates required message field", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();
        await page.getByRole("button", { name: /new greeting/i }).click();

        // Submit without entering message
        await page.getByRole("button", { name: /create greeting/i }).click();

        // Validation error should appear
        await expect(page.getByText(/message is required/i)).toBeVisible();
    });

    test("creates a new greeting successfully", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();
        await page.getByRole("button", { name: /new greeting/i }).click();

        // Fill form
        await page.getByLabel(/message/i).fill("New E2E greeting");
        await page.getByLabel(/recipient/i).fill("E2E Tester");

        // Submit
        await page.getByRole("button", { name: /create greeting/i }).click();

        // Form should close after successful creation
        await expect(page.getByRole("heading", { name: /create new greeting/i })).not.toBeVisible({
            timeout: 5000,
        });
    });
});

// ============================================================
// Edit Greeting Tests
// ============================================================

test.describe("Edit Greeting", () => {
    test("opens edit form with pre-filled data", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Click first edit button
        const editButtons = page.getByRole("button", { name: /edit/i });
        await editButtons.first().click();

        // Edit form should be visible
        await expect(page.getByRole("heading", { name: /edit greeting/i })).toBeVisible();

        // Form should be pre-filled with greeting data
        const messageInput = page.getByLabel(/message/i);
        await expect(messageInput).toHaveValue("Hello, World!");
    });

    test("updates greeting and closes form", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Click edit
        await page.getByRole("button", { name: /edit/i }).first().click();

        // Modify message
        const messageInput = page.getByLabel(/message/i);
        await messageInput.clear();
        await messageInput.fill("Updated greeting message");

        // Submit
        await page.getByRole("button", { name: /update/i }).click();

        // Form should close
        await expect(page.getByRole("heading", { name: /edit greeting/i })).not.toBeVisible({
            timeout: 5000,
        });
    });

    test("cancel edit closes form without saving", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        await page.getByRole("button", { name: /edit/i }).first().click();
        await expect(page.getByRole("heading", { name: /edit greeting/i })).toBeVisible();

        // Cancel
        await page.getByRole("button", { name: /cancel/i }).click();

        // Form should close
        await expect(page.getByRole("heading", { name: /edit greeting/i })).not.toBeVisible();
    });
});

// ============================================================
// Delete Greeting Tests
// ============================================================

test.describe("Delete Greeting", () => {
    test("shows confirmation and deletes greeting", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Set up dialog handler before clicking delete
        page.on("dialog", (dialog) => dialog.accept());

        // Click delete
        await page
            .getByRole("button", { name: /delete/i })
            .first()
            .click();

        // After deletion, the list should refresh (mocked to still show all items)
        await expect(page.getByText("Hello, World!")).toBeVisible();
    });

    test("cancel delete keeps greeting", async ({ page }) => {
        await mockAllGreetingsApis(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Set up dialog handler to dismiss
        page.on("dialog", (dialog) => dialog.dismiss());

        // Click delete
        await page
            .getByRole("button", { name: /delete/i })
            .first()
            .click();

        // Greeting should still be visible
        await expect(page.getByText("Hello, World!")).toBeVisible();
    });
});

// ============================================================
// Pagination Tests
// ============================================================

test.describe("Pagination", () => {
    test("displays pagination info", async ({ page }) => {
        await mockGreetingsApi(page);
        await page.goto("/");

        await expect(page.getByText("Hello, World!")).toBeVisible();

        // Pagination info visible
        await expect(page.getByText(/page 1 of 1/i)).toBeVisible();
        await expect(page.getByText(/3 total/i)).toBeVisible();
    });

    test("pagination buttons work with multiple pages", async ({ page }) => {
        let currentPage = 0;
        await page.route("**/api/v1/greetings**", (route) => {
            const url = new URL(route.request().url());
            const pageParam = url.searchParams.get("page");
            currentPage = pageParam ? parseInt(pageParam) : 0;

            route.fulfill({
                status: 200,
                contentType: "application/json",
                body: JSON.stringify({
                    data: [mockGreetings[currentPage] || mockGreetings[0]],
                    meta: {
                        pageNumber: currentPage,
                        pageSize: 1,
                        totalElements: 3,
                        totalPages: 3,
                    },
                }),
            });
        });

        await page.goto("/");
        await expect(page.getByText("Hello, World!")).toBeVisible();
        await expect(page.getByText(/page 1 of 3/i)).toBeVisible();

        // Previous should be disabled on first page
        const prevButton = page.getByRole("button", { name: /previous/i });
        await expect(prevButton).toBeDisabled();

        // Click next
        const nextButton = page.getByRole("button", { name: /next/i });
        await nextButton.click();

        // Should now show page 2
        await expect(page.getByText(/page 2 of 3/i)).toBeVisible();

        // Previous should now be enabled
        await expect(prevButton).not.toBeDisabled();
    });
});
