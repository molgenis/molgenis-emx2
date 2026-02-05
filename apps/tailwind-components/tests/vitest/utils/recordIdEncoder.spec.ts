import { describe, expect, it } from "vitest";
import {
  encodeRecordId,
  decodeRecordId,
} from "../../../app/utils/recordIdEncoder";

describe("recordIdEncoder", () => {
  it("encodes simple key to query string", () => {
    expect(encodeRecordId({ name: "poodle" })).toBe("name=poodle");
  });

  it("decodes simple query string", () => {
    expect(decodeRecordId("name=poodle")).toEqual({ name: "poodle" });
  });

  it("roundtrip encode/decode", () => {
    const pk = { id: "1", name: "test" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("handles special chars", () => {
    const pk = { name: "foo/bar=baz" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("handles composite keys", () => {
    const pk = { schema: "pet store", table: "Pet", id: "123" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("encodes nested objects with dot notation", () => {
    const pk = { resource: { name: "Athlete" }, name: "foo" };
    const encoded = encodeRecordId(pk);
    expect(encoded).toContain("resource.name=Athlete");
    expect(encoded).toContain("name=foo");
  });

  it("decodes dot notation to nested objects", () => {
    const result = decodeRecordId("resource.name=Athlete&name=foo");
    expect(result).toEqual({ resource: { name: "Athlete" }, name: "foo" });
  });

  it("roundtrip nested objects", () => {
    const pk = { resource: { name: "Athlete" }, name: "something" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("handles deeply nested objects", () => {
    const pk = { a: { b: { c: "deep" } } };
    const encoded = encodeRecordId(pk);
    expect(encoded).toBe("a.b.c=deep");
    expect(decodeRecordId(encoded)).toEqual(pk);
  });

  it("skips null and undefined values", () => {
    const pk = { name: "test", nullVal: null, undefinedVal: undefined };
    const encoded = encodeRecordId(pk);
    expect(encoded).toBe("name=test");
  });

  it("URL encodes special characters", () => {
    const pk = { name: "hello world", path: "a/b&c=d" };
    const encoded = encodeRecordId(pk);
    expect(encoded).toBe("name=hello+world&path=a%2Fb%26c%3Dd");
  });

  it("URL encodes unicode characters", () => {
    const pk = { name: "café" };
    const encoded = encodeRecordId(pk);
    expect(encoded).toBe("name=caf%C3%A9");
    expect(decodeRecordId(encoded)).toEqual(pk);
  });
});
