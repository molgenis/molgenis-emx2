import { describe, it, expect } from "vitest";
import { keySlug, type KeyObject } from "../../../app/utils/navigationUtils";

describe("keySlug", () => {
  it("returns value for single key", () => {
    const input: KeyObject = {
      a: "foo",
    };

    expect(keySlug(input)).toBe("foo");
  });

  it("joins multiple string values with dash", () => {
    const input: KeyObject = {
      a: "foo",
      b: "bar",
      c: "baz",
    };

    expect(keySlug(input)).toBe("foo-bar-baz");
  });

  it("handles nested objects", () => {
    const input: KeyObject = {
      a: "foo",
      b: {
        c: "bar",
      },
    };

    expect(keySlug(input)).toBe("foo-bar");
  });

  it("handles deeply nested objects", () => {
    const input: KeyObject = {
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
    const input: KeyObject = {
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
    const input: KeyObject = {};

    expect(keySlug(input)).toBe("");
  });

  it("preserves Object.values insertion order", () => {
    const input: KeyObject = {
      first: "one",
      second: {
        third: "two",
      },
      fourth: "three",
    };

    expect(keySlug(input)).toBe("one-two-three");
  });
});
