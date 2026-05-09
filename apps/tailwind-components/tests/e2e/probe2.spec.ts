import { test, expect } from "@playwright/test";
test("probe filter counts and groupby", async ({ page }) => {
  const allRequests: any[] = [];
  const groupByRequests: any[] = [];

  page.on("request", (req) => {
    if (req.method() === "POST" && req.url().includes("/graphql")) {
      try {
        const body = JSON.parse(req.postData() ?? "{}");
        allRequests.push(body);
        if (body.query && body.query.includes("_groupBy")) {
          groupByRequests.push(body);
        }
      } catch {}
    }
  });

  const gqlResps: any[] = [];
  page.on("response", async (resp) => {
    if (resp.request().method() === "POST" && resp.url().includes("/graphql")) {
      try {
        const json = await resp.json();
        gqlResps.push(json);
      } catch {}
    }
  });

  await page.goto("/catalogue-demo/Resources/", { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);

  console.log("===== GRAPHQL REQUEST BODIES =====");
  for (let i = 0; i < allRequests.length; i++) {
    const q = allRequests[i].query || "";
    const firstLine = q.split("\n")[0].slice(0, 80);
    console.log(`REQ ${i}: ${firstLine}`);
  }

  console.log("\n===== GROUPBY REQUESTS =====");
  console.log(`Found ${groupByRequests.length} _groupBy calls`);
  for (const req of groupByRequests) {
    console.log("GROUPBY_QUERY:", JSON.stringify(req).slice(0, 600));
  }

  console.log("\n===== FILTER SIDEBAR OPTIONS =====");
  // Try to locate filter options
  const optionBoxes = page.locator("input[type='checkbox']");
  const optionCount = await optionBoxes.count();
  console.log(`Found ${optionCount} checkboxes`);

  // Look for spans/divs with counts like "Option (5)"
  const allText = await page.locator("body").textContent();
  const countPattern = /\w+\s*\(\d+\)/g;
  const matches = allText?.match(countPattern) || [];
  console.log(
    `Found ${matches.length} count patterns (like "Option (5)"):`,
    matches.slice(0, 20)
  );

  // Try to find filter sections
  const sections = page.locator("[role='button'][aria-expanded]");
  const sectionCount = await sections.count();
  console.log(`Found ${sectionCount} expandable sections`);

  // Check for tree/list structure in sidebar
  const sidebar = page.locator("aside").first();
  const innerText = await sidebar.textContent().catch(() => "");
  console.log("SIDEBAR_TEXT_FIRST_500:", innerText.slice(0, 500));
});
