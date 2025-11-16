import { test, expect } from "@playwright/test";

test("loads app and toggles theme", async ({ page }) => {
    await page.goto("/");

    // Title visible
    await expect(
        page.getByRole("heading", { name: /react \+ typescript → spring/i }),
    ).toBeVisible();

    // API base text rendered
    await expect(page.getByText(/API base:/i)).toBeVisible();

    // Theme toggle works
    const toggle = page.getByRole("button", { name: /switch to dark theme/i });
    await toggle.click();

    const main = page.locator("main");
    await expect(main).toHaveAttribute("data-theme", "dark");
});
