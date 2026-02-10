import { describe, it, expect } from "vitest";

import {
  transformToKeyObject,
  buildValueKey,
  resourceIdPath,
  buildCanonicalUrl,
} from "./urlHelpers";

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

describe("resourceIdPath", () => {
  it("should take a KeyObject and transform path section that identifies a single entity( table row )", () => {
    const someKey = {
      name: "foo",
      category: {
        name: "bar",
        section: {
          id: "section 1",
        },
      },
    };

    expect(resourceIdPath(someKey)).toEqual(
      `foo-bar-section 1?keys={"name":"foo","category":{"name":"bar","section":{"id":"section 1"}}}`
    );
  });
});

describe("buildCanonicalUrl", () => {
  it("should replace a sub-catalogue path with /all/", () => {
    const url = new URL(
      "https://molgeniscatalogue.org/NCC/collections/OncoLifeS"
    );
    expect(buildCanonicalUrl(url, { catalogue: "NCC" })).toBe(
      "https://molgeniscatalogue.org/all/collections/OncoLifeS"
    );
  });

  it("should leave /all/ paths unchanged", () => {
    const url = new URL(
      "https://molgeniscatalogue.org/all/collections/OncoLifeS"
    );
    expect(buildCanonicalUrl(url, { catalogue: "all" })).toBe(
      "https://molgeniscatalogue.org/all/collections/OncoLifeS"
    );
  });

  it("should keep the page parameter but strip filter params", () => {
    const url = new URL(
      "https://molgeniscatalogue.org/all/collections?page=2&conditions=foo&view=compact"
    );
    expect(buildCanonicalUrl(url, { catalogue: "all" })).toBe(
      "https://molgeniscatalogue.org/all/collections?page=2"
    );
  });

  it("should strip all query parameters when there is no page param", () => {
    const url = new URL(
      "https://molgeniscatalogue.org/all/collections?conditions=foo&view=compact"
    );
    expect(buildCanonicalUrl(url, { catalogue: "all" })).toBe(
      "https://molgeniscatalogue.org/all/collections"
    );
  });

  it("should only replace the first path segment when catalogue name appears in resource id", () => {
    const url = new URL("https://molgeniscatalogue.org/NCC/collections/NCC");
    expect(buildCanonicalUrl(url, { catalogue: "NCC" })).toBe(
      "https://molgeniscatalogue.org/all/collections/NCC"
    );
  });

  it("should handle nested sub-catalogue resource paths", () => {
    const url = new URL(
      "https://molgeniscatalogue.org/ATHLETE/collections/ABCD"
    );
    expect(buildCanonicalUrl(url, { catalogue: "ATHLETE" })).toBe(
      "https://molgeniscatalogue.org/all/collections/ABCD"
    );
  });
});
