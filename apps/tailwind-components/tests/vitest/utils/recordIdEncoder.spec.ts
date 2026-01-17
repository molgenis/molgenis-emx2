import { describe, expect, it } from "vitest";
import {
  encodeRecordId,
  decodeRecordId,
} from "../../../app/utils/recordIdEncoder";

describe("recordId", () => {
  it("roundtrip encode/decode", () => {
    const pk = { id: 1, name: "test" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("handles special chars", () => {
    const pk = { name: "foo/bar=baz" };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });

  it("handles composite keys", () => {
    const pk = { schema: "pet store", table: "Pet", id: 123 };
    expect(decodeRecordId(encodeRecordId(pk))).toEqual(pk);
  });
});
