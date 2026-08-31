import { test as setup } from "@playwright/test";
import * as path from "path";
import * as fs from "fs";

const authDir = path.join(__dirname, "../../playwright/.auth");
const authFile = path.join(authDir, "user.json");

// Ensure auth directory exists
if (!fs.existsSync(authDir)) {
  fs.mkdirSync(authDir, { recursive: true });
}

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
