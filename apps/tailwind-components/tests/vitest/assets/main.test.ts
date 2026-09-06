import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const mainCss = readFileSync(
  join(__dirname, "../../../app/assets/css/main.css"),
  "utf-8"
);

function surfaceInvertedRule(): string {
  const start = mainCss.indexOf(".surface-inverted {");
  const end = mainCss.indexOf("}", start);
  return mainCss.slice(start, end);
}

// Every role .surface-inverted must repoint so text stays readable on an
// inverted surface, and the token it repoints to.
const REPOINTED_ROLES: [role: string, target: string][] = [
  ["--text-color-link", "--text-color-link-inverted"],
  ["--text-color-icon-neutral", "--text-color-link-inverted"],
  ["--text-color-record-heading", "--text-color-title"],
  ["--text-color-record-label", "--text-color-title"],
  ["--text-color-record-value", "--text-color-title"],
  ["--text-color-table-column-header", "--text-color-title"],
  ["--text-color-table-row", "--text-color-title"],
  ["--text-color-pagination", "--text-color-pagination-inverted"],
];

describe(".surface-inverted in main.css", () => {
  it("repoints every text role to a token that stays readable on an inverted surface", () => {
    const rule = surfaceInvertedRule();
    const missing = REPOINTED_ROLES.filter(
      ([role, target]) => !rule.includes(`${role}: var(${target});`)
    ).map(([role]) => role);

    expect(
      missing,
      `missing or wrong repoint for: ${missing.join(", ")}`
    ).toEqual([]);
  });
});
