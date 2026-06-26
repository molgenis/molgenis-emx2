import { describe, it, expect } from "vitest";
import { compileTemplate } from "../../../app/utils/compileTemplate";

describe("compileTemplate", () => {
  it("compiles a valid simple element to a component", () => {
    const result = compileTemplate("<div>hello</div>");
    expect(result.error).toBeNull();
    expect(result.component).not.toBeNull();
  });

  it("compiles a valid element with attributes", () => {
    const result = compileTemplate('<span class="foo">bar</span>');
    expect(result.error).toBeNull();
    expect(result.component).not.toBeNull();
  });

  it("returns error shape for v-if without expression", () => {
    const result = compileTemplate("<div v-if></div>");
    expect(result.component).toBeNull();
    expect(result.error).not.toBeNull();
    expect(typeof result.error).toBe("string");
  });

  it("returns error shape for invalid JS expression in binding", () => {
    const result = compileTemplate('<div :class="=> foo"></div>');
    expect(result.component).toBeNull();
    expect(result.error).not.toBeNull();
    expect(typeof result.error).toBe("string");
  });

  it("never throws on malformed input", () => {
    expect(() => compileTemplate("<< invalid {{")).not.toThrow();
    expect(() => compileTemplate("")).not.toThrow();
    expect(() => compileTemplate("<div v-for></div>")).not.toThrow();
  });

  it("registers design-system components so resolveComponent can find Button", () => {
    const result = compileTemplate("<div>test</div>");
    expect(result.error).toBeNull();
    const def = result.component as { components?: Record<string, unknown> };
    expect(def.components).toBeDefined();
    expect(def.components).toHaveProperty("Button");
  });
});
