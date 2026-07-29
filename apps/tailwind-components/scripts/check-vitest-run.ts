import { existsSync, readFileSync, realpathSync } from "node:fs";
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
  const [reportFile, ...unexpectedArguments] = process.argv.slice(2);
  if (reportFile === undefined || unexpectedArguments.length > 0) {
    console.error(
      "usage: check-vitest-run.ts <vitest json report>; run a single file with `pnpm exec vitest run <file>`"
    );
    process.exit(2);
  }

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
}
