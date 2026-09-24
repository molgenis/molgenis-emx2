import { describe, it, expect } from "vitest";
import { keySlug } from "../../../app/utils/navigationUtils";

describe("keySlug", () => {
  it("returns value for single key", () => {
    const input = {
      a: "foo",
    };

    expect(keySlug(input)).toBe("foo");
  });

  it("joins multiple string values with dash", () => {
    const input = {
      a: "foo",
      b: "bar",
      c: "baz",
    };

    expect(keySlug(input)).toBe("foo-bar-baz");
  });

  it("handles nested objects", () => {
    const input = {
      a: "foo",
      b: {
        c: "bar",
      },
    };

    expect(keySlug(input)).toBe("foo-bar");
  });

  it("handles deeply nested objects", () => {
    const input = {
      a: "foo",
      b: {
        c: {
          d: "bar",
        },
      },
    };

    expect(keySlug(input)).toBe("foo-bar");
  });

  it("handles mixed nesting and multiple values", () => {
    const input = {
      a: "foo",
      b: {
        c: "bar",
        d: "baz",
      },
      e: "qux",
    };

    expect(keySlug(input)).toBe("foo-bar-baz-qux");
  });

  it("returns empty string for empty object", () => {
    const input = {};

    expect(keySlug(input)).toBe("");
  });

  it("preserves Object.values insertion order", () => {
    const input = {
      first: "one",
      second: {
        third: "two",
      },
      fourth: "three",
    };

    expect(keySlug(input)).toBe("one-two-three");
  });
});
