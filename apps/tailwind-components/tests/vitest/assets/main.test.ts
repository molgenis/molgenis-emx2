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

describe(".surface-inverted in main.css", () => {
  it("repoints link and icon tokens to the theme's inverted set", () => {
    const rule = surfaceInvertedRule();
    expect(rule).toContain(
      "--text-color-link: var(--text-color-link-inverted);"
    );
    expect(rule).toContain(
      "--text-color-icon-neutral: var(--text-color-link-inverted);"
    );
  });

  it("repoints record heading, label and value to a token the theme already defines for a colour it owns", () => {
    const rule = surfaceInvertedRule();
    expect(rule).toContain(
      "--text-color-record-heading: var(--text-color-title);"
    );
    expect(rule).toContain(
      "--text-color-record-label: var(--text-color-title);"
    );
    expect(rule).toContain(
      "--text-color-record-value: var(--text-color-title);"
    );
  });

  it("repoints the pager's range label to its defined inverted counterpart", () => {
    const rule = surfaceInvertedRule();
    expect(rule).toContain(
      "--text-color-pagination: var(--text-color-pagination-inverted);"
    );
  });

  it("repoints table column headers and rows to the same theme-owned colour, for lack of an inverted table-text token", () => {
    const rule = surfaceInvertedRule();
    expect(rule).toContain(
      "--text-color-table-column-header: var(--text-color-title);"
    );
    expect(rule).toContain("--text-color-table-row: var(--text-color-title);");
  });
});
