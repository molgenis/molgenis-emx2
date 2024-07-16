const { chromium } = require('playwright');
import { test, expect } from '@playwright/test';

// If testing through UI, run auth.setup.spec.ts first!
test.beforeAll(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('/apps/central/');
  await page.getByRole('button', { name: '' }).click();
  await page.getByLabel('name').click();
  await page.getByLabel('name').fill('test_FAIR');
  await page.getByLabel('template').selectOption('FAIR_DATA_HUB');
  await page.getByRole('button', { name: 'Create database' }).click();
  await expect(page.getByText('Schema test_FAIR created')).toBeVisible({ timeout: 30000 });
});

test.afterAll(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('/apps/central/');
  await page.locator('tr').filter({ hasText: 'test_FAIR' }).locator('button').nth(1).click();;
  await page.getByRole('button', { name: 'Delete database' }).click();

});

test('Does the custom RDF change when field is edited', async ({ page }) => {
  await page.goto('/test_FAIR/api/rdf/Catalog');
  await expect(page.locator('pre')).toContainText('@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . @prefix xsd: <http://www.w3.org/2001/XMLSchema#> . @prefix owl: <http://www.w3.org/2002/07/owl#> . @prefix sio: <http://semanticscience.org/resource/> . @prefix qb: <http://purl.org/linked-data/cube#> . @prefix skos: <http://www.w3.org/2004/02/skos/core#> . @prefix dcterms: <http://purl.org/dc/terms/> . @prefix dcat: <http://www.w3.org/ns/dcat#> . @prefix foaf: <http://xmlns.com/foaf/0.1/> . @prefix vcard: <http://www.w3.org/2006/vcard/ns#> . @prefix org: <http://www.w3.org/ns/org#> . @prefix fdp-o: <https://w3id.org/fdp/fdp-o#> .');
  await page.goto('/test_FAIR/settings/#/settings');
  await page.getByRole('button', { name: '' }).click();
  await page.locator('textarea').click();
  await page.locator('textarea').fill('@prefix dcterms: <http://purl.org/dc/terms/> .\n\n<http://example.com/> dcterms:title "Example website" .');
  await page.getByRole('button', { name: 'Edit Setting' }).click();
  await page.goto('/test_FAIR/api/rdf/Catalog');
  await expect(page.locator('pre')).toContainText('@prefix dcterms: <http://purl.org/dc/terms/> . <http://example.com/> dcterms:title "Example website" .', { timeout: 10000 });
});