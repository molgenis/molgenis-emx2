import { test as setup } from "@playwright/test";

const authFile = "playwright/.auth/diamond.json";

setup("authenticate for diamond tests", async ({ request }) => {
  // Authenticate via GraphQL API to set cookies
  console.log("\n1. Signing in via GraphQL API");
  const signInResponse = await request.post("/api/graphql", {
    data: {
      query: `
        mutation {
          signin(email: "admin", password: "admin") {
            status, message
          }
        }
      `,
    },
  });

  const result = await signInResponse.json();
  console.log(`   Status: ${result.data?.signin?.status}`);

  // Save auth state (cookies will be preserved)
  console.log("\n2. Saving auth state to " + authFile);
  await request.storageState({ path: authFile });
  console.log("   Done");
});
