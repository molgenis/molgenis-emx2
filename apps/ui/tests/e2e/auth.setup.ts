import { test as setup } from "@playwright/test";

const authFile = "playwright/.auth/user.json";

setup("authenticate", async ({ request }) => {
  await request.post("/api/graphql", {
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

  // Save cookies + localStorage to JSON
  await request.storageState({ path: authFile });
});
