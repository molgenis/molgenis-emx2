const test = require("node:test");
const assert = require("node:assert/strict");

const { proxiedOrigin } = require("./dev-proxy-origin.js");

const TARGET = "https://emx2.dev.molgenis.org/pet store/graphql";

test("request from the dev server itself gets the target origin", () => {
  assert.equal(
    proxiedOrigin(
      { host: "localhost:3000", origin: "http://localhost:3000" },
      TARGET
    ),
    "https://emx2.dev.molgenis.org"
  );
});

test("referer is used when origin is absent", () => {
  assert.equal(
    proxiedOrigin(
      { host: "localhost:3000", referer: "http://localhost:3000/pet%20store" },
      TARGET
    ),
    "https://emx2.dev.molgenis.org"
  );
});

test("request from another site keeps its origin", () => {
  assert.equal(
    proxiedOrigin(
      { host: "localhost:3000", origin: "https://evil.example" },
      TARGET
    ),
    undefined
  );
});

test("request from another local port keeps its origin", () => {
  assert.equal(
    proxiedOrigin(
      { host: "localhost:3000", origin: "http://localhost:4000" },
      TARGET
    ),
    undefined
  );
});

test("request without origin or referer is left alone", () => {
  assert.equal(proxiedOrigin({ host: "localhost:3000" }, TARGET), undefined);
});

test("unparsable origin is left alone", () => {
  assert.equal(
    proxiedOrigin({ host: "localhost:3000", origin: "null" }, TARGET),
    undefined
  );
});
