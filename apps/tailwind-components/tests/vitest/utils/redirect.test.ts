import { describe, expect, test } from "vitest";
import { sanitizeRedirectPath } from "../../../app/utils/redirect";

describe("sanitizeRedirectPath", () => {
  test("keeps a path on the current host", () => {
    expect(sanitizeRedirectPath("/pet store/tables")).toBe("/pet store/tables");
  });

  test("keeps the query and hash of a path", () => {
    expect(sanitizeRedirectPath("/tables?page=2#row-3")).toBe(
      "/tables?page=2#row-3"
    );
  });

  test("rejects an absolute url to another host", () => {
    expect(() => sanitizeRedirectPath("https://evil.example/x")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects an absolute url to the current host", () => {
    // the router navigates by path, so even our own origin is spelled out wrong
    expect(() => sanitizeRedirectPath("http://localhost:8080/tables")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a protocol relative url", () => {
    expect(() => sanitizeRedirectPath("//evil.example/x")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a backslash disguised as a path", () => {
    // browsers read the backslash as a slash, making this `//evil.example`
    expect(() => sanitizeRedirectPath("/\\evil.example")).toThrow(
      "Invalid redirect"
    );
    expect(() => sanitizeRedirectPath("\\\\evil.example")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a url with a control character inside the authority", () => {
    expect(() => sanitizeRedirectPath("/\t/evil.example")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a javascript url", () => {
    expect(() => sanitizeRedirectPath("javascript:alert(1)")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a relative path the router cannot resolve", () => {
    expect(() => sanitizeRedirectPath("tables/pet")).toThrow(
      "Invalid redirect"
    );
  });

  test("rejects a value that is not a string", () => {
    expect(() => sanitizeRedirectPath(undefined)).toThrow("Invalid redirect");
    expect(() => sanitizeRedirectPath(null)).toThrow("Invalid redirect");
    expect(() => sanitizeRedirectPath(42)).toThrow("Invalid redirect");
  });

  test("uses the first value of a repeated query parameter", () => {
    expect(sanitizeRedirectPath(["/tables", "//evil.example"])).toBe("/tables");
    expect(() => sanitizeRedirectPath(["//evil.example", "/tables"])).toThrow(
      "Invalid redirect"
    );
  });

  test("trims surrounding whitespace", () => {
    expect(sanitizeRedirectPath("  /tables  ")).toBe("/tables");
  });
});
