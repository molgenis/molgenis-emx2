import { describe, it, expect } from "vitest";
import {
  extractDemoSource,
  extractTemplateBody,
} from "../../../app/utils/demoSource";

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

describe("extractTemplateBody", () => {
  it("returns the inner content of a simple template block", () => {
    const source = `<template>\n  <div>hello</div>\n</template>`;
    const result = extractTemplateBody(source);
    expect(result).toContain("<div>hello</div>");
    expect(result).not.toContain("<template>");
    expect(result).not.toContain("</template>");
  });

  it("dedents the template body", () => {
    const source = `<template>\n  <div>\n    <span>text</span>\n  </div>\n</template>`;
    const result = extractTemplateBody(source);
    expect(result.startsWith("<div>")).toBe(true);
    expect(result).not.toMatch(/^ {4}/m);
    expect(result).toContain("  <span>text</span>");
  });

  it("returns trimmed output (no leading/trailing blank lines)", () => {
    const source = `<template>\n\n  <p>hi</p>\n\n</template>`;
    const result = extractTemplateBody(source);
    expect(result).toBe(result.trim());
  });

  it("returns empty string when no template block exists", () => {
    expect(extractTemplateBody("")).toBe("");
    expect(extractTemplateBody("<script>const x = 1</script>")).toBe("");
  });

  it("ignores script block content before the template", () => {
    const source = `<script setup lang="ts">\nconst x = 1;\n</script>\n\n<template>\n  <p>content</p>\n</template>`;
    const result = extractTemplateBody(source);
    expect(result).toContain("<p>content</p>");
    expect(result).not.toContain("const x");
  });

  it("handles nested template directives correctly", () => {
    const source = `<template>\n  <div>\n    <template v-if="show">\n      <span>inner</span>\n    </template>\n  </div>\n</template>`;
    const result = extractTemplateBody(source);
    expect(result).toContain('<template v-if="show">');
    expect(result).toContain("<span>inner</span>");
    expect(result.startsWith("<div>")).toBe(true);
  });

  it("handles deeply nested template slots", () => {
    const source = `<template>\n  <Comp>\n    <template #default>\n      <template v-for="x in xs">\n        <span>{{ x }}</span>\n      </template>\n    </template>\n  </Comp>\n</template>`;
    const result = extractTemplateBody(source);
    expect(result).toContain("<Comp>");
    expect(result).toContain('<template #default>');
    expect(result).toContain('<template v-for="x in xs">');
  });
});
