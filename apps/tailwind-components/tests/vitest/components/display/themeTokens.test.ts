import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const FORBIDDEN_PATTERNS: { name: string; pattern: RegExp }[] = [
  { name: "hex colour", pattern: /#[0-9a-fA-F]{3,8}\b/ },
  { name: "text-white", pattern: /\btext-white\b/ },
  { name: "text-black", pattern: /\btext-black\b/ },
  { name: "bg-gray-", pattern: /\bbg-gray-\d+\b/ },
];

const displayDir = join(__dirname, "../../../../app/components/display");
const recordsDir = join(displayDir, "records");

// The record-display family this suite guards: the Records.vue wrapper plus
// every dumb layout under display/records/. Sibling files in display/ (e.g.
// CodeBlock.vue, List.vue) belong to other components and are out of scope.
const componentFiles = [
  join(displayDir, "Records.vue"),
  ...readdirSync(recordsDir)
    .filter((fileName) => fileName.endsWith(".vue"))
    .map((fileName) => join(recordsDir, fileName)),
];

describe("display/**/*.vue theme tokens", () => {
  it("found at least one component file to check", () => {
    expect(componentFiles.length).toBeGreaterThan(0);
  });

  it.each(componentFiles)("%s uses only theme-token colours", (filePath) => {
    const contents = readFileSync(filePath, "utf-8");

    for (const { name, pattern } of FORBIDDEN_PATTERNS) {
      expect(pattern.test(contents), `${filePath} contains a ${name}`).toBe(
        false
      );
    }
  });
});
