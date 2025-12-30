import { fileURLToPath } from "url";
import fs from "fs/promises";
import { resolve } from "path";

const __dirname = fileURLToPath(new URL("..", import.meta.url));
const inputFile = resolve(__dirname, "sourceCodeMap.json");
const outputFile = resolve(__dirname, ".pa11yci");

const pa11yBaseConfig = {
  defaults: {
    reporters: [
      "cli",
      [
        "pa11y-ci-reporter-html",
        {
          destination: ".reports/",
          includeZeroIssues: true,
        },
      ],
    ],
  },
  urls: [],
};

async function updateTestUrls() {
  const stories = await fs.readFile(inputFile, "utf8");
  try {
    console.log("✅ Refreshing site map...");
    const json = JSON.parse(stories);
    const paths = Object.keys(json);
    pa11yBaseConfig.urls = paths.map(
      (path) => `http://localhost:3000${path.replace(".vue", "")}`
    );

    console.log("✅ Saving paths to .pa11y config...");
    await fs.writeFile(outputFile, JSON.stringify(pa11yBaseConfig, null, 2));
  } catch (err) {
    console.error("❌ Error writing to file:", err);
  }
}

updateTestUrls();
