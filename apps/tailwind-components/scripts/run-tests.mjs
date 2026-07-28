import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, rmSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { unexecutedTestsFailure } from "./test-run-completeness.mjs";

const require = createRequire(import.meta.url);
const vitestBin = join(
  dirname(require.resolve("vitest/package.json")),
  require("vitest/package.json").bin.vitest
);
const reportDirectory = fileURLToPath(
  new URL("../node_modules/.cache", import.meta.url)
);
const reportFile = join(reportDirectory, "vitest-run-report.json");

mkdirSync(reportDirectory, { recursive: true });
rmSync(reportFile, { force: true });

const vitest = spawnSync(
  process.execPath,
  [
    vitestBin,
    "run",
    "--reporter=default",
    "--reporter=json",
    `--outputFile.json=${reportFile}`,
    ...process.argv.slice(2),
  ],
  { stdio: "inherit" }
);

if (!existsSync(reportFile)) {
  console.error(
    `run-tests: vitest wrote no run report at ${reportFile} — the suite cannot be shown to have run`
  );
  process.exit(1);
}

const failure = unexecutedTestsFailure(
  JSON.parse(readFileSync(reportFile, "utf8"))
);
if (failure) {
  console.error(`run-tests: ${failure}`);
  process.exit(1);
}

process.exit(vitest.status ?? 1);
