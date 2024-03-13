import { test, expect } from '@playwright/test';

test('Catalogue test number 1: Naïve user of the data catalogue', async ({ page }) => {
  // Step 1
  await page.goto('https://data-catalogue-acc.molgeniscloud.org/testCatalogue/ssr-catalogue/');
  await expect(page.locator('h1')).toContainText('European Health Research Data and Sample Catalogue');
  // Step 2
  await expect(page.getByRole('cell', { name: 'testNetworkofNetworks' })).toBeVisible();
  // Step 3
  await page.getByText('testNetworkofNetworks').click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  await expect(page.getByRole('main')).toContainText('Welcome to the catalogue of testNetworkofNetworks: name for test network of networks. Select one of the content categories listed below.');
  await expect(page.getByRole('main')).toContainText('Cohorts3');
  await expect(page.getByRole('main')).toContainText('Variables3');
  await expect(page.getByRole('main')).toContainText('Networks2');
  // Step 3a
  await expect(page.getByRole('main')).toContainText('700 Participants');
  await expect(page.getByRole('main')).toContainText('250 Samples');
  await expect(page.getByRole('main')).toContainText('Longitudinal 67%');
  // Step 3b
  await expect(page.getByRole('link', { name: 'Go to home' })).toBeVisible();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  await expect(page.getByRole('link', { name: 'overview' })).toBeVisible();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Cohorts' })).toBeVisible();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Variables' })).toBeVisible();
  await expect(page.getByRole('navigation').getByRole('link', { name: 'Networks' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'More' })).toBeVisible();
  // Step 4
  await page.getByRole('link', { name: 'Go to home' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  // Step 5
  await page.getByRole('link', { name: 'overview' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  // Step 6
  
  await page.getByRole('navigation').getByRole('link', { name: 'Cohorts' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworkscohorts');

  await expect(page.getByRole('list')).toContainText('acronym for test cohort 1Name for test cohort 1');
  await expect(page.getByRole('list')).toContainText('acronym for test cohort 2Name for test cohort 2');
  await expect(page.getByRole('list')).toContainText('testCohort3');
  // Step 7

  await page.getByRole('link', { name: 'Go to home' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  // Step 8

  await page.getByRole('navigation').getByRole('link', { name: 'Variables' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworksvariables');
  await expect(page.getByRole('main')).toContainText('testVarCategorical_categorical test variable');
  await expect(page.getByRole('main')).toContainText('testVarRepeats_test variable with repeats');
  await expect(page.getByRole('main')).toContainText('testVarNoRepeatstest variable without repeats');
  // Step 9

  await page.getByRole('link', { name: 'overview' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  // Step 10

  await page.getByRole('navigation').getByRole('link', { name: 'Networks' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworksnetworks');
  await expect(page.getByRole('main')).toContainText('acronym for test network1name for test network1');
  await expect(page.getByRole('main')).toContainText('acronym for test network2name for test network2');
  // Step 11

  await page.locator('header').filter({ hasText: 'acronym for test network1name' }).getByRole('button').click();
  await expect(page.locator('h1')).toContainText('acronym for test network1');
  await expect(page.getByRole('main')).toContainText('name for test network1');
  await expect(page.locator('#Description')).toContainText('Description');
  await expect(page.getByRole('main')).toContainText('https://www.molgenis.org');
  await expect(page.locator('#cohorts')).toContainText('Cohorts');
  await expect(page.locator('#cohorts')).toContainText('Name for test cohort 1');
  await expect(page.locator('#cohorts')).toContainText('Name for test cohort 2');
  await expect(page.locator('#cohorts')).toContainText('testCohort3');
  await expect(page.locator('#cohorts')).toContainText('testCohort4');
  await expect(page.locator('#variables')).toContainText('Variables');
  await expect(page.locator('#variables')).toContainText('testVarCategorical_');
  await expect(page.locator('#variables')).toContainText('testVarNoRepeats');
  await expect(page.locator('#variables')).toContainText('testVarRepeats_');
  // Step 12

  await page.getByRole('link', { name: 'overview' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
  // Step 13

  await page.getByRole('link', { name: 'About' }).click();
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworksNetworks');
  await expect(page.locator('h1')).toContainText('acronym for test network of networks');
  await expect(page.getByRole('main')).toContainText('name for test network of networks');
  // Step 14

  await page.getByRole('link', { name: 'Other catalogues' }).click();
  await expect(page.locator('h1')).toContainText('European Health Research Data and Sample Catalogue');
  await expect(page.getByRole('main')).toContainText('A collaborative effort to integrate the catalogues of diverse EU research projects and networks to accelerate reuse and improve citizens health.');
  await expect(page.getByRole('main')).toContainText('Thematic catalogues');
  await expect(page.getByRole('main')).toContainText('testNetwork1');
  await expect(page.getByRole('main')).toContainText('Project catalogues');
  await expect(page.getByRole('main')).toContainText('testNetworkofNetworks');
});