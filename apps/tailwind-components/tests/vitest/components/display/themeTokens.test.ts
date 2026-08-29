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

const dataComponentFiles = readdirSync(displayDir).filter(
  (fileName) => fileName.startsWith("Data") && fileName.endsWith(".vue")
);

describe("display/Data*.vue theme tokens", () => {
  it("found at least one Data*.vue file to check", () => {
    expect(dataComponentFiles.length).toBeGreaterThan(0);
  });

  it.each(dataComponentFiles)(
    "%s uses only theme-token colours",
    (fileName) => {
      const contents = readFileSync(join(displayDir, fileName), "utf-8");

      for (const { name, pattern } of FORBIDDEN_PATTERNS) {
        expect(pattern.test(contents), `${fileName} contains a ${name}`).toBe(
          false
        );
      }
    }
  );
});
