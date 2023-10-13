import { describe, it, expect } from "vitest";

import { transformToKeyObject, buildValueKey } from "./urlHelpers";

describe("transformToKeyObject", () => {
  it("should take a string that represents a key and transform it into a KeyObject ", () => {
    const someKey = {
      name: "foo",
      category: {
        name: "bar",
      },
      section: {
        id: "section 1",
      },
    };
    const stringKey = JSON.stringify(someKey);
    expect(transformToKeyObject(stringKey)).toEqual(someKey);
  });
});

describe("buildValueKey", () => {
  it("should take a KeyObject and transform it into a string that represents a key ( values only) ", () => {
    const someKey = {
      name: "foo",
      category: {
        name: "bar",
        section: {
          id: "section 1",
        },
      },
    };

    expect(buildValueKey(someKey)).toEqual("foo-bar-section 1");
  });
});
