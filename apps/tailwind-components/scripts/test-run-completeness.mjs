const executedStatuses = ["passed", "failed"];

function unexecutedCountOf(fileResult) {
  return fileResult.assertionResults.filter(
    (assertion) => !executedStatuses.includes(assertion.status)
  ).length;
}

export function unexecutedTestsFailure(report) {
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
