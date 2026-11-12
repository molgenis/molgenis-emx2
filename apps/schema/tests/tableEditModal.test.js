const test = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");

const modalSource = fs.readFileSync(
  path.join(__dirname, "..", "src", "components", "TableEditModal.vue"),
  "utf8"
);

function sectionBetween(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  assert.notStrictEqual(start, -1, `no '${startMarker}' section found`);
  const end = source.indexOf(endMarker, start);
  assert.notStrictEqual(end, -1, `no '${endMarker}' after '${startMarker}'`);
  return source.slice(start, end);
}

test("the table edit modal resolves the parent it opens with", () => {
  const methods = sectionBetween(modalSource, "  methods: {", "  created()");
  assert.ok(
    /resolveInheritName\(/.test(methods),
    "expected reset() to resolve the parent through resolveInheritName"
  );
});

test("the table edit modal never rewrites the parent while rendering", () => {
  const computed = sectionBetween(modalSource, "  computed: {", "  methods: {");
  assert.ok(
    !/this\.table\.inheritName\s*=(?!=)/.test(computed),
    "a computed that assigns inheritName re-points the table being edited"
  );
});
