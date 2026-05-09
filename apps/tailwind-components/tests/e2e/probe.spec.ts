import { test, expect } from "@playwright/test";
test("probe filter sidebar state", async ({ page }) => {
  const consoleMsgs: string[] = [];
  page.on("console", (m) => consoleMsgs.push(`[${m.type()}] ${m.text()}`));
  const gql: any[] = [];
  page.on("request", (req) => {
    if (req.method() === "POST" && req.url().includes("/graphql")) {
      try {
        gql.push({ url: req.url(), body: JSON.parse(req.postData() ?? "{}") });
      } catch {}
    }
  });
  page.on("response", async (resp) => {
    if (resp.request().method() === "POST" && resp.url().includes("/graphql")) {
      try {
        const json = await resp.json();
        console.log("GQL_RESP:", JSON.stringify(json).slice(0, 300));
      } catch {}
    }
  });
  await page.goto("/catalogue-demo/Resources/", { waitUntil: "networkidle" });
  await page.waitForTimeout(1000);
  console.log("URL:", page.url());
  console.log("TITLE:", await page.title());
  // dump filter sidebar DOM structure
  const sidebar = page
    .locator("aside, [class*='sidebar' i], [class*='filter' i]")
    .first();
  console.log(
    "SIDEBAR_HTML_FIRST_2K:",
    (await sidebar.innerHTML().catch(() => "no sidebar")).slice(0, 2000)
  );
  // count visible filter options (checkbox labels with counts)
  const checkboxLabels = await page.locator("label").allTextContents();
  console.log("LABEL_COUNT:", checkboxLabels.length);
  console.log("LABELS:", JSON.stringify(checkboxLabels.slice(0, 30)));
  // any console errors?
  console.log("CONSOLE_MSGS:");
  for (const m of consoleMsgs.slice(0, 30)) console.log("  ", m);
  console.log("GQL_REQ_COUNT:", gql.length);
  for (const r of gql.slice(0, 10)) {
    console.log("GQL_REQ:", JSON.stringify(r.body).slice(0, 400));
  }
  // snapshot
  await page.screenshot({ path: "/tmp/probe.png", fullPage: true });
});
