import { describe, expect, test } from "vitest";
import { unexecutedTestsFailure } from "../../../scripts/check-vitest-run.ts";

function fileResult(name: string, statuses: string[]) {
  return {
    name,
    status: statuses.includes("failed") ? "failed" : "passed",
    assertionResults: statuses.map((status, index) => ({
      title: `case ${index}`,
      status,
    })),
  };
}

function report(testResults: ReturnType<typeof fileResult>[]) {
  const assertions = testResults.flatMap((file) => file.assertionResults);
  const countOf = (status: string) =>
    assertions.filter((assertion) => assertion.status === status).length;
  return {
    numTotalTests: assertions.length,
    numPassedTests: countOf("passed"),
    numFailedTests: countOf("failed"),
    numPendingTests: countOf("skipped") + countOf("pending"),
    numTodoTests: countOf("todo"),
    testResults,
  };
}

describe("test run completeness:", () => {
  test("a run whose collected tests all passed or failed executed the whole suite", () => {
    const failure = unexecutedTestsFailure(
      report([
        fileResult("/apps/example/tests/a.spec.ts", ["passed", "passed"]),
        fileResult("/apps/example/tests/b.spec.ts", ["passed", "failed"]),
      ])
    );

    expect(failure).toBeNull();
  });

  test("tests skipped by a dead setup hook are reported as never executed, with the file that lost them", () => {
    const failure = unexecutedTestsFailure(
      report([
        fileResult("/apps/example/tests/a.spec.ts", ["passed", "passed"]),
        fileResult("/apps/example/tests/dead.spec.ts", [
          "skipped",
          "skipped",
          "skipped",
        ]),
      ])
    );

    expect(failure).toEqual(
      "3 of 5 collected tests never executed, so the reported pass count is not the whole suite " +
        "(a test file whose setup hook dies reports its tests as skipped, not failed):\n" +
        "  /apps/example/tests/dead.spec.ts: 3 of 3 never executed"
    );
  });

  test("every file that lost tests is named, not just the first", () => {
    const failure = unexecutedTestsFailure(
      report([
        fileResult("/apps/example/tests/a.spec.ts", ["skipped"]),
        fileResult("/apps/example/tests/b.spec.ts", ["passed", "skipped"]),
      ])
    );

    expect(failure).toContain(
      "/apps/example/tests/a.spec.ts: 1 of 1 never executed"
    );
    expect(failure).toContain(
      "/apps/example/tests/b.spec.ts: 1 of 2 never executed"
    );
  });

  test("a test that ran and failed is not counted as never executed", () => {
    const failure = unexecutedTestsFailure(
      report([
        fileResult("/apps/example/tests/failing.spec.ts", ["failed", "failed"]),
        fileResult("/apps/example/tests/dead.spec.ts", ["skipped"]),
      ])
    );

    expect(failure).toEqual(
      "1 of 3 collected tests never executed, so the reported pass count is not the whole suite " +
        "(a test file whose setup hook dies reports its tests as skipped, not failed):\n" +
        "  /apps/example/tests/dead.spec.ts: 1 of 1 never executed"
    );
  });

  test("a run that collected no tests at all is a failure, not a vacuous pass", () => {
    const failure = unexecutedTestsFailure(report([]));

    expect(failure).toEqual(
      "no tests were collected — the suite cannot be shown to have run"
    );
  });
});
