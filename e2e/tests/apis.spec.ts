import { test, expect, request, APIRequestContext } from '@playwright/test';

let apiContext: APIRequestContext;

test.beforeAll(async () => {
  apiContext = await request.newContext();
})


test("rdf api works", async ({ page }) => {
  const response = await apiContext.get(`/pet%20store/api/rdf`);
  expect(response.ok()).toBeTruthy();
});