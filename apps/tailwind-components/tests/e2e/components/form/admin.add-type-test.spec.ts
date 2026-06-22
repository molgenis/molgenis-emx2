import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/tailwind-components/#/";

test("it should be able to fill out all input types", async ({ page }) => {
  await page.goto(route + "form/AddModal.story?schema=type+test&table=Types");
  await page.getByRole("button", { name: "Add Types" }).click();

  // String
  await page.getByRole("textbox", { name: "string type Required" }).click();
  await page
    .getByRole("textbox", { name: "string type Required" })
    .fill("some string");

  // String array
  await page
    .locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
    .fill("some string in an array");
  await page
    .locator('[id="type test-Types-stringArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
    .fill("another string in an array");

  // Text
  await page.getByRole("textbox", { name: "text type" }).click();
  await page.getByRole("textbox", { name: "text type" }).fill("lots of text");

  // Text array
  await page
    .locator('[id="type test-Types-textArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-textArrayType-form-field-input_0"]')
    .fill("more text");
  await page
    .locator('[id="type test-Types-textArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-textArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-textArrayType-form-field-input_1"]')
    .fill("in an array");

  // Json
  await page.getByRole("textbox", { name: "json type" }).click();
  await page
    .getByRole("textbox", { name: "json type" })
    .fill('{ "some": "json"}');

  // Email
  await page.getByRole("textbox", { name: "email type" }).click();
  await page
    .getByRole("textbox", { name: "email type" })
    .fill("info@molgenis.net");

  // Email array
  await page.getByRole("textbox", { name: "Input an email address" }).click();
  await page
    .getByRole("textbox", { name: "Input an email address" })
    .fill("more@email.com");
  await page
    .locator('[id="type test-Types-emailArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-emailArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-emailArrayType-form-field-input_1"]')
    .fill("root@google.com");

  // Hyperlink
  await page.getByRole("textbox", { name: "hyperlink type" }).click();
  await page
    .getByRole("textbox", { name: "hyperlink type" })
    .fill("https://www.molgenis.org");

  // Hyperlink array
  await page.getByRole("textbox", { name: "Input a hyperlink" }).click();
  await page
    .getByRole("textbox", { name: "Input a hyperlink" })
    .fill("http://molgenis.net");
  await page
    .locator('[id="type test-Types-hyperlinkArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-hyperlinkArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-hyperlinkArrayType-form-field-input_1"]')
    .fill("http://www.google.com");

  // Integer
  await page.getByRole("textbox", { name: "int type", exact: true }).click();
  await page.getByRole("textbox", { name: "int type", exact: true }).fill("-5");

  // Integer array
  await page
    .locator('[id="type test-Types-intArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-intArrayType-form-field-input_0"]')
    .fill("6");
  await page
    .locator('[id="type test-Types-intArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-intArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-intArrayType-form-field-input_1"]')
    .fill("37");

  // Long
  await page.getByRole("textbox", { name: "long type" }).click();
  await page.getByRole("textbox", { name: "long type" }).fill("6778");

  // Long array
  await page
    .locator('[id="type test-Types-longArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-longArrayType-form-field-input_0"]')
    .fill("8787");
  await page
    .locator('[id="type test-Types-longArrayType-form-field"]')
    .getByRole("button", { name: "Add an additional item" })
    .click();
  await page
    .locator('[id="type test-Types-longArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-longArrayType-form-field-input_1"]')
    .fill("7");

  // Decimal
  await page.getByRole("textbox", { name: "decimal type" }).click();
  await page.getByRole("textbox", { name: "decimal type" }).fill("-1.1");

  // Decimal array
  await page
    .locator('[id="type test-Types-decimalArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-decimalArrayType-form-field-input_0"]')
    .fill("2.2");
  await page
    .locator(
      '[id="type test-Types-decimalArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator('[id="type test-Types-decimalArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-decimalArrayType-form-field-input_1"]')
    .fill("3.3");

  // Non negative integer
  await page.getByRole("textbox", { name: "non negative int type" }).click();
  await page.getByRole("textbox", { name: "non negative int type" }).fill("37");

  // Non negative integer array
  await page
    .locator(
      '[id="type test-Types-nonNegativeIntArrayType-form-field-input_0"]'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-nonNegativeIntArrayType-form-field-input_0"]'
    )
    .fill("42");
  await page
    .locator(
      '[id="type test-Types-nonNegativeIntArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-nonNegativeIntArrayType-form-field-input_1"]'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-nonNegativeIntArrayType-form-field-input_1"]'
    )
    .fill("1337");

  // Boolean
  await page.locator("circle").nth(1).click();

  // Boolean array
  await page.locator("circle").nth(5).click();
  await page
    .locator(
      '[id="type test-Types-boolArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-boolArrayType-form-field-input_1-radio-group"] > div:nth-child(2) > .text-body-base > .mt-1\\.5 > .fill-transparent'
    )
    .click();

  // UUID
  await page.getByRole("textbox", { name: "uuid type" }).click();
  await page
    .getByRole("textbox", { name: "uuid type" })
    .fill("123e4567-e89b-12d3-a456-426614174000");

  // UUID array
  await page
    .locator('[id="type test-Types-uuidArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-uuidArrayType-form-field-input_0"]')
    .fill("123e4567-e89b-12d3-a456-426614174000");
  await page
    .locator(
      '[id="type test-Types-uuidArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator('[id="type test-Types-uuidArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-uuidArrayType-form-field-input_1"]')
    .fill("123e4567-e89b-12d3-a456-426614174000");

  // File
  // await page.getByRole("button", { name: "Browse" }).click();
  // await page
  //   .getByRole("button", { name: "Browse" })
  //   .setInputFiles("Dockerfile");

  // Date
  await page
    .locator('[id="dp-input-type test-Types-dateType-form-field-input"]')
    .click();
  await page.locator('[data-test-id="dp-2026-06-01"]').getByText("1").click();

  // Date array
  await page
    .locator('[id="dp-input-type test-Types-dateArrayType-form-field-input_0"]')
    .click();
  await page.locator('[data-test-id="dp-2026-06-02"]').getByText("2").click();
  await page
    .locator(
      '[id="type test-Types-dateArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator('[id="dp-input-type test-Types-dateArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="dp-input-type test-Types-dateArrayType-form-field-input_1"]')
    .fill("1987-10-04");

  // Date time
  await page
    .locator('[id="dp-input-type test-Types-datetimeType-form-field-input"]')
    .click();
  await page.locator('[data-test-id="dp-2026-06-03"]').getByText("3").click();

  // Date time array
  await page
    .locator(
      '[id="dp-input-type test-Types-datetimeArrayType-form-field-input_0"]'
    )
    .click();
  await page.locator('[data-test-id="dp-2026-06-11"]').getByText("11").click();
  await page
    .locator(
      '[id="type test-Types-datetimeArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator(
      '[id="dp-input-type test-Types-datetimeArrayType-form-field-input_1"]'
    )
    .click();

  await page.locator('[data-test-id="dp-2026-06-07"]').getByText("7").click();

  // Period
  await page.getByRole("textbox", { name: "period type" }).click();
  await page.getByRole("textbox", { name: "period type" }).fill("P23Y12M12D");

  // Period array
  await page
    .locator('[id="type test-Types-periodArrayType-form-field-input_0"]')
    .click();
  await page
    .locator('[id="type test-Types-periodArrayType-form-field-input_0"]')
    .fill("P37Y11M10D");
  await page
    .locator(
      '[id="type test-Types-periodArrayType-form-field"] > div:nth-child(3) > .flex.items-center.justify-center'
    )
    .click();
  await page
    .locator('[id="type test-Types-periodArrayType-form-field-input_1"]')
    .click();
  await page
    .locator('[id="type test-Types-periodArrayType-form-field-input_1"]')
    .fill("P13Y37M42D");

  // REFS
  // REF
  await page
    .locator('[id="type test-Types-refType-form-field-input-radio-group"]')
    .getByText("AT", { exact: true })
    .click();

  // Select
  await page
    .locator('[id="search-for-type test-Types-selectType-form-field-input"]')
    .click();
  await page
    .locator('[id="type test-Types-selectType-form-field-input-radio-group"]')
    .getByText("FR")
    .click();

  // Select Label
  await page
    .locator('[id="type test-Types-radioType-form-field-input-radio-group"]')
    .getByText("HU")
    .click();
  await page
    .locator(
      '[id="search-for-type test-Types-selectTypeLabel-form-field-input"]'
    )
    .click();

  // Radio
  await page
    .locator(
      '[id="type test-Types-selectTypeLabel-form-field-input-radio-group"]'
    )
    .getByText("Belgium")
    .click();

  // Radio label
  await page
    .locator(
      '[id="type test-Types-radioTypeLabel-form-field-input-radio-group"] > div > .text-body-base > .mt-1\\.5 > .fill-transparent'
    )
    .first()
    .click();

  // Ref array
  await page
    .locator(
      '[id="type test-Types-refArrayType-form-field-input-checkbox-group"]'
    )
    .getByText("CY")
    .click();
  await page
    .locator(
      '[id="type test-Types-refArrayType-form-field-input-checkbox-group"]'
    )
    .getByText("GR", { exact: true })
    .click();

  // Checkbox
  await page
    .locator(
      '[id="type test-Types-checkboxType-form-field-input-checkbox-group"]'
    )
    .getByText("ES", { exact: true })
    .click();
  await page
    .locator(
      '[id="type test-Types-checkboxType-form-field-input-checkbox-group"]'
    )
    .getByText("DE", { exact: true })
    .click();
  await page
    .locator(
      '[id="type test-Types-checkboxType-form-field-input-checkbox-group"]'
    )
    .getByText("CY", { exact: true })
    .click();

  // Checkbox label
  await page
    .locator(
      '[id="type test-Types-checkboxTypeLabel-form-field-input-checkbox-group"]'
    )
    .getByText("Greece")
    .click();
  await page
    .locator(
      '[id="type test-Types-checkboxTypeLabel-form-field-input-checkbox-group"]'
    )
    .getByText("France")
    .click();
  await page
    .locator(
      '[id="type test-Types-checkboxTypeLabel-form-field-input-checkbox-group"]'
    )
    .getByText("Germany")
    .click();

  // Multiselect
  await page
    .locator(
      '[id="type test-Types-multiselectType-form-field-input-ref"] > .flex.items-center.justify-between'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-multiselectType-form-field-input-checkbox-group"] span'
    )
    .filter({ hasText: "CY" })
    .click();
  await page
    .locator(
      '[id="type test-Types-multiselectType-form-field-input-checkbox-group"] > div:nth-child(5) > .text-body-base > .w-\\[20px\\] > .fill-input'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-multiselectType-form-field-input-checkbox-group"]'
    )
    .getByText("LT", { exact: true })
    .click();

  // Multiselect label
  // await page
  //   .locator(
  //     '[id="type test-Types-multiselectTypeLabel-form-field-input-ref"] > .flex.items-center.justify-between > div:nth-child(2) > svg:nth-child(2)'
  //   )
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-multiselectTypeLabel-form-field-input-checkbox-group"] > div:nth-child(4) > .text-body-base > .w-\\[20px\\] > .fill-input'
  //   )
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-multiselectTypeLabel-form-field-input-checkbox-group"]'
  //   )
  //   .getByText("Cyprus")
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-multiselectTypeLabel-form-field-input-checkbox-group"]'
  //   )
  //   .getByText("Cyprus")
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-multiselectTypeLabel-form-field-input-checkbox-group"]'
  //   )
  //   .getByText("Belgium")
  //   .click();

  // Ref back
  await page
    .locator(
      '[id="type test-Types-refBackType-form-field-input-radio-group"] > div:nth-child(6) > .text-body-base'
    )
    .click();

  // Ontology small
  await page
    .locator(
      '[id="type test-Types-ontologySmallType-form-field-input-ontology"]'
    )
    .getByText("Spain")
    .click();

  // Ontology large
  await page
    .locator(
      '[id="type test-Types-ontologyLargeType-form-field-input-ontology"] > .flex.items-center.justify-between'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeType-form-field-input-ontology"]'
    )
    .getByText("Europe")
    .click();

  // Ontology small array

  await page
    .locator(
      '[id="type test-Types-ontologySmallArrayType-form-field-input-ontology"] span'
    )
    .filter({ hasText: "Greece" })
    .click();
  await page
    .locator(
      "li:nth-child(6) > .flex.justify-start > .text-body-base > .w-\\[20px\\] > .fill-input"
    )
    .first()
    .click();

  // Ontology large array
  await page
    .locator(
      '[id="type test-Types-ontologyLargeArrayType-form-field-input-ontology"] > .flex.items-center.justify-between'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeArrayType-form-field-input-ontology"] span'
    )
    .filter({ hasText: "Belgium" })
    .click();
  await page
    .locator(
      ".absolute > .pl-4 > .text-search-filter-group-title > li:nth-child(4) > .flex.justify-start > .text-body-base > .w-\\[20px\\] > .fill-input"
    )
    .first()
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeArrayType-form-field-input-ontology"] label'
    )
    .filter({ hasText: "Canada" })
    .click();

  // Ontology small tree
  await page
    .locator(
      '[id="type test-Types-ontologySmallTreeType-form-field-input-ontology"]'
    )
    .getByText("Switzerland")
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologySmallTreeType-form-field-input-ontology"]'
    )
    .getByText("Asia")
    .click();

  // Ontology large tree
  // await page
  //   .locator(
  //     '[id="type test-Types-ontologyLargeTreeType-form-field-input-ontology"] > .flex.items-center.justify-between'
  //   )
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-ontologyLargeTreeType-form-field-input-ontology"]'
  //   )
  //   .getByText("Asia")
  //   .click();
  // await page
  //   .locator("div")
  //   .filter({ hasText: /^Asia$/ })
  //   .nth(1)
  //   .click();
  // await page
  //   .locator(
  //     '[id="type test-Types-ontologyLargeTreeType-form-field-input-ontology"]'
  //   )
  //   .getByRole("button", { name: "expand EU" })
  //   .click();
  // await page
  //   .locator(
  //     ".absolute > fieldset > .text-search-filter-group-title.pb-2 > li:nth-child(3) > .text-search-filter-group-title > li:nth-child(2) > .flex.justify-start > .text-body-base > .block"
  //   )
  //   .click();

  // Ontology small tree array
  await page
    .locator(
      '[id="type test-Types-ontologySmallTreeArrayType-form-field-input-ontology"]'
    )
    .getByText("Uganda")
    .click();
  await page
    .locator(
      ".text-search-filter-group-title.pb-2.pl-4 > li:nth-child(3) > div:nth-child(2) > .text-body-base > .w-\\[20px\\] > .fill-input"
    )
    .first()
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologySmallTreeArrayType-form-field-input-ontology"]'
    )
    .getByText("Czech Republic")
    .click();

  // Ontology large tree array
  await page
    .locator(
      '[id="type test-Types-ontologyLargeTreeArrayType-form-field-input-ontology"] > .flex.items-center.justify-between'
    )
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeTreeArrayType-form-field-input-ontology"]'
    )
    .getByText("Asia")
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeTreeArrayType-form-field-input-ontology"]'
    )
    .getByRole("button", { name: "expand EU" })
    .click();
  await page
    .locator(
      '[id="type test-Types-ontologyLargeTreeArrayType-form-field-input-ontology"]'
    )
    .getByText("Bulgaria")
    .click();
  await page.goto(
    "http://localhost:3000/form/AddModal.story?schema=type+test&table=Types"
  );
  await page.getByRole("button", { name: "Add Types" }).click();

  // Save the form
  await page.getByRole("button", { name: "Save", exact: true }).click();
});
