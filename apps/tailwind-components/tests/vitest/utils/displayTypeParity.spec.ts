import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { DISPLAY_TYPES } from "../../../../metadata-utils/src/types";

const backendEnumPath = resolve(
  process.cwd(),
  "../../backend/molgenis-emx2/src/main/java/org/molgenis/emx2/DisplayType.java"
);

function readBackendDisplayTypeConstants(): string[] {
  const source = readFileSync(backendEnumPath, "utf8")
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/\/\/[^\n]*/g, "");
  const enumBody = source.match(/enum\s+DisplayType\s*\{([^}]*)\}/);
  if (!enumBody) {
    throw new Error(`No DisplayType enum body in ${backendEnumPath}`);
  }
  return enumBody[1]!
    .split(",")
    .map((constant) => constant.trim())
    .filter(Boolean)
    .sort();
}

describe("DISPLAY_TYPES", () => {
  it("names exactly the constants of the backend DisplayType enum", () => {
    expect([...DISPLAY_TYPES].sort()).toEqual(
      readBackendDisplayTypeConstants()
    );
  });
});
