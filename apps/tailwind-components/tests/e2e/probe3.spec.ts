import { test, expect } from "@playwright/test";
test("probe errors and network", async ({ page }) => {
  const errors: string[] = [];
  const warnings: string[] = [];

  page.on("console", (m) => {
    if (m.type() === "error") errors.push(m.text());
    if (m.type() === "warning") warnings.push(m.text());
  });

  page.on("pageerror", (err) => {
    errors.push(`PAGE_ERROR: ${err.message || String(err)}`);
  });

  const responses: any[] = [];
  page.on("response", async (resp) => {
    if (resp.url().includes("/graphql")) {
      try {
        const text = await resp.text();
        const data = JSON.parse(text);
        const errors = data.errors || [];
        const hasData = !!data.data;
        responses.push({
          status: resp.status(),
          hasErrors: errors.length > 0,
          hasData,
          errorMsgs: errors.map((e: any) => e.message || e).slice(0, 2),
        });
      } catch {}
    }
  });

  await page.goto("/catalogue-demo/Resources/", { waitUntil: "networkidle" });
  await page.waitForTimeout(2000);

  console.log("ERRORS:", errors.slice(0, 20));
  console.log("WARNINGS:", warnings.slice(0, 10));
  console.log(
    "\nGQL_RESPONSES:",
    JSON.stringify(responses, null, 2).slice(0, 1000)
  );

  // Try to inspect Vue app state directly
  const vueState = await page.evaluate(() => {
    // @ts-ignore
    return {
      windowKeys: Object.keys(window)
        .filter((k) => k.startsWith("__"))
        .slice(0, 5),
    };
  });
  console.log("VUE_STATE_KEYS:", vueState.windowKeys);
});
