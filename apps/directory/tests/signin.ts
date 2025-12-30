export async function signin(page) {
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Password").click();
  await page.getByPlaceholder("Password").fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
}
