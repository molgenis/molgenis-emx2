import "regenerator-runtime/runtime";

const { chromium } = require("playwright");

test("test schema", async () => {
  const browser = await chromium.launch({
    headless: false,
  });
  const context = await browser.newContext();

  // Open new page
  const page = await context.newPage();

  // Go to http://localhost:9092/
  await page.goto("http://localhost:9092/");

  // Go to http://localhost:9092/#/
  await page.goto("http://localhost:9092/#/");

  // Click text="Sign in"
  await page.click('text="Sign in"');

  // Click input[placeholder="Enter email adress"]
  await page.click('input[placeholder="Enter email adress"]');

  // Fill input[placeholder="Enter email adress"]
  await page.fill('input[placeholder="Enter email adress"]', "admin");

  // Click input[placeholder="Enter password"]
  await page.click('input[placeholder="Enter password"]');

  // Fill input[placeholder="Enter password"]
  await page.fill('input[placeholder="Enter password"]', "admin");

  // Click //button[2][normalize-space(.)='Sign in']
  await page.click("//button[2][normalize-space(.)='Sign in']");

  // Click text=/.*create table.*/
  await page.click("text=/.*create table.*/");

  // Click //span[normalize-space(.)='‌‌']/button/i
  await page.click("//span[normalize-space(.)='‌‌']/button/i");

  // Click //div[normalize-space(.)='Clear']/input
  await page.click("//div[normalize-space(.)='Clear']/input");

  // Fill //div[normalize-space(.)='Clear']/input
  await page.fill("//div[normalize-space(.)='Clear']/input", "tablex");

  // Click //thead[normalize-space(.)='columnNamecolumnTypekeyrequiredrefTable mappedBy refLinkjsonldTypedescription']/th[1]/button/i
  await page.click(
    "//thead[normalize-space(.)='columnNamecolumnTypekeyrequiredrefTable mappedBy refLinkjsonldTypedescription']/th[1]/button/i"
  );

  // Click text="Save"
  await page.click('text="Save"');

  // Click //div[starts-with(normalize-space(.), 'tablex Open form editor Inherits: ‌‌ Description: ‌‌ jsonldType: ‌‌ columnNameco')]/button[1]/i
  await page.click(
    "//div[starts-with(normalize-space(.), 'tablex Open form editor Inherits: ‌‌ Description: ‌‌ jsonldType: ‌‌ columnNameco')]/button[1]/i"
  );

  // Click text="Save"
  await page.click('text="Save"');

  // Close page
  await page.close();

  // ---------------------
  await context.close();
  await browser.close();
});
