import { describe, it, expect } from "vitest";
import { extractDemoSource } from "../../../app/utils/demoSource";

const multiBlockFixture = `
<template>
  <div>
    <Demo id="alpha" title="Alpha">
      <button>Alpha button</button>
    </Demo>
    <Demo id="beta" title="Beta">
      <div class="flex gap-4">
        <button>Beta A</button>
        <button>Beta B</button>
      </div>
    </Demo>
  </div>
</template>
`;

const extraAttrsFixture = `
<template>
  <Demo title="Extra" id="extra" class="mt-4" data-testid="extra-demo">
    <span>content</span>
  </Demo>
</template>
`;

const dedentFixture = `
<template>
  <Demo id="deep">
      <div class="container">
        <span>text</span>
      </div>
  </Demo>
</template>
`;

describe("extractDemoSource", () => {
  it("extracts the correct block by id from a multi-Demo fixture", () => {
    const result = extractDemoSource(multiBlockFixture, "alpha");
    expect(result).toContain("<button>Alpha button</button>");
    expect(result).not.toContain("Beta");
  });

  it("extracts the second block correctly and not the first", () => {
    const result = extractDemoSource(multiBlockFixture, "beta");
    expect(result).toContain('<div class="flex gap-4">');
    expect(result).toContain("<button>Beta A</button>");
    expect(result).toContain("<button>Beta B</button>");
    expect(result).not.toContain("Alpha");
  });

  it("returns empty string for a missing id", () => {
    expect(extractDemoSource(multiBlockFixture, "gamma")).toBe("");
  });

  it("returns empty string for empty source", () => {
    expect(extractDemoSource("", "alpha")).toBe("");
  });

  it("handles extra attributes before and after id on the Demo tag", () => {
    const result = extractDemoSource(extraAttrsFixture, "extra");
    expect(result).toContain("<span>content</span>");
  });

  it("strips common leading indentation (dedent)", () => {
    const result = extractDemoSource(dedentFixture, "deep");
    expect(result).not.toMatch(/^ {4}/m);
    expect(result.startsWith("<div")).toBe(true);
    expect(result).toContain("<span>text</span>");
  });

  it("dedented result is trimmed (no leading/trailing blank lines)", () => {
    const result = extractDemoSource(multiBlockFixture, "alpha");
    expect(result).toBe(result.trim());
  });

  it("id with special regex characters is escaped safely", () => {
    const source = `<Demo id="a.b">content</Demo>`;
    expect(extractDemoSource(source, "a.b")).toBe("content");
    expect(extractDemoSource(source, "axb")).toBe("");
  });
});
