import { spawnSync } from "node:child_process";
import {
  existsSync,
  mkdirSync,
  readFileSync,
  realpathSync,
  rmSync,
} from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const executedStatuses = ["passed", "failed"];

export interface AssertionResult {
  status: string;
}

export interface FileResult {
  name: string;
  assertionResults: AssertionResult[];
}

export interface TestRunReport {
  numTotalTests: number;
  numPassedTests: number;
  numFailedTests: number;
  testResults: FileResult[];
}

function unexecutedCountOf(fileResult: FileResult): number {
  return fileResult.assertionResults.filter(
    (assertion) => !executedStatuses.includes(assertion.status)
  ).length;
}

export function unexecutedTestsFailure(report: TestRunReport): string | null {
  const collected = report.numTotalTests;
  if (collected === 0) {
    return "no tests were collected — the suite cannot be shown to have run";
  }
  const executed = report.numPassedTests + report.numFailedTests;
  if (executed === collected) {
    return null;
  }
  const filesThatLostTests = report.testResults
    .filter((fileResult) => unexecutedCountOf(fileResult) > 0)
    .map(
      (fileResult) =>
        `  ${fileResult.name}: ${unexecutedCountOf(fileResult)} of ${
          fileResult.assertionResults.length
        } never executed`
    )
    .join("\n");
  return (
    `${collected - executed} of ${collected} collected tests never executed, ` +
    "so the reported pass count is not the whole suite " +
    "(a test file whose setup hook dies reports its tests as skipped, not failed):\n" +
    filesThatLostTests
  );
}

const entryPoint = process.argv[1];
const invokedDirectly =
  entryPoint !== undefined &&
  realpathSync(entryPoint) === realpathSync(fileURLToPath(import.meta.url));

if (invokedDirectly) {
  const require = createRequire(import.meta.url);
  const vitestManifest = require("vitest/package.json") as {
    bin: { vitest: string };
  };
  const vitestBin = join(
    dirname(require.resolve("vitest/package.json")),
    vitestManifest.bin.vitest
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
      `check-vitest-run: vitest wrote no run report at ${reportFile} — the suite cannot be shown to have run`
    );
    process.exit(1);
  }

  const failure = unexecutedTestsFailure(
    JSON.parse(readFileSync(reportFile, "utf8")) as TestRunReport
  );
  if (failure) {
    console.error(`check-vitest-run: ${failure}`);
    process.exit(1);
  }

  process.exit(vitest.status ?? 1);
}
