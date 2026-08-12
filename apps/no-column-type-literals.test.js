const test = require("node:test");
const assert = require("node:assert/strict");

const {
  findColumnTypeLiterals,
  toExecutableSource,
  isScannedFile,
  reconcileWithAllowlist,
  scanWorkspace,
} = require("./no-column-type-literals.js");

const allowlist = require("./no-column-type-literals.allowlist.json");

const scan = (source) =>
  findColumnTypeLiterals(source, "tailwind-components/app/utils/example.ts");

const scanVue = (source) =>
  findColumnTypeLiterals(
    toExecutableSource(
      source,
      "tailwind-components/app/components/Example.vue"
    ),
    "tailwind-components/app/components/Example.vue"
  );

test("a refback family name is a violation on its own", () => {
  const violations = scan(`if (column.columnType === "REFBACK") return true;`);
  assert.deepEqual(
    violations.map((violation) => violation.names),
    [["REFBACK"]]
  );
});

test("a single ref flavor name is left to per-widget dispatch", () => {
  assert.deepEqual(scan(`if (columnType === "SELECT") return 2;`), []);
});

test("two ref flavor names in one expression are a family check", () => {
  const violations = scan(
    `const isSelectType = ["SELECT", "MULTISELECT"].includes(columnType);`
  );
  assert.deepEqual(
    violations.map((violation) => violation.names),
    [["MULTISELECT", "SELECT"]]
  );
});

test("a lone layout name is a violation when the line dispatches on a columnType", () => {
  const violations = scan(
    `const i = columns.findIndex((c) => c.columnType === "HEADING" && c.id === "x");`
  );
  assert.deepEqual(
    violations.map((violation) => violation.names),
    [["HEADING"]]
  );
});

test("a lone layout name spread over a multi-line attribute still sees the columnType", () => {
  const source = [
    `<template>`,
    `  <div`,
    `    :class="`,
    `      column.columnType === 'HEADING'`,
    `        ? 'text-heading-4xl'`,
    `        : 'text-heading-5xl'`,
    `    "`,
    `  />`,
    `</template>`,
  ].join("\n");
  assert.deepEqual(
    scanVue(source).map((violation) => violation.names),
    [["HEADING"]]
  );
});

test("a layout name belonging to another type's vocabulary is not a column type check", () => {
  assert.deepEqual(
    scan(`const sections = legend.filter((s) => s.type === "SECTION");`),
    []
  );
  assert.deepEqual(scan(`  type: "HEADING",`), []);
});

test("a lone layout name is missed when the line never says columnType, by design", () => {
  assert.deepEqual(scan(`if (col.type === "HEADING") return true;`), []);
});

test("commented-out code is not executable source", () => {
  const source = [
    `// if (column.columnType === "REFBACK") return true;`,
    `/* ["HEADING", "SECTION"].includes(x) */`,
    `const url = "https://example.org//REFBACK";`,
  ].join("\n");
  assert.deepEqual(
    findColumnTypeLiterals(
      toExecutableSource(source, "tailwind-components/app/utils/example.ts"),
      "tailwind-components/app/utils/example.ts"
    ),
    []
  );
});

test("a docs demo block is not executable source but a template is", () => {
  const source = [
    `<template>`,
    `  <ValueRefBack v-if="metadata.columnType === 'REFBACK'" />`,
    `</template>`,
    `<docs>`,
    `  <Example columnType="REFBACK" />`,
    `</docs>`,
    `<style scoped>`,
    `  /* REFBACK */`,
    `</style>`,
  ].join("\n");
  assert.deepEqual(
    scanVue(source).map((violation) => violation.line),
    [2]
  );
});

test("a family check spread over a multi-line attribute is one violation", () => {
  const source = [
    `<template>`,
    `  <div`,
    `    v-if="`,
    `      column.refTableName &&`,
    `      (column.columnType === 'REF' ||`,
    `        column.columnType === 'REF_ARRAY')`,
    `    "`,
    `  />`,
    `</template>`,
  ].join("\n");
  assert.deepEqual(
    scanVue(source).map((violation) => violation.names),
    [["REF", "REF_ARRAY"]]
  );
});

test("the canonical predicate family and the type union may name column types", () => {
  for (const owner of [
    "metadata-utils/src/fieldHelpers.ts",
    "metadata-utils/src/types.ts",
    "schema/src/columnTypes.js",
  ]) {
    assert.equal(isScannedFile(owner), false, owner);
  }
});

test("tests, stories and generated output are not production dispatch", () => {
  for (const skipped of [
    "tailwind-components/tests/vitest/utils/displayUtils.spec.ts",
    "metadata-utils/test/test-resources/metadata.ts",
    "tailwind-components/app/components/Input.story.vue",
    "molgenis-components/src/components/mockDatasets.js",
    "ui/node_modules/whatever/index.ts",
    "catalogue/.nuxt/types.ts",
    "tailwind-components/README.md",
  ]) {
    assert.equal(isScannedFile(skipped), false, skipped);
  }
});

test("production sources of every frontend package are scanned", () => {
  for (const scanned of [
    "tailwind-components/app/utils/buildGqlFilter.ts",
    "molgenis-components/src/components/filters/FilterWells.vue",
    "schema/src/components/ColumnEditModal.vue",
    "ui/app/pages/[schema]/[table]/index.vue",
    "catalogue/app/utils/filterUtils.ts",
    "metadata-utils/src/typeUtils.ts",
  ]) {
    assert.equal(isScannedFile(scanned), true, scanned);
  }
});

const violation = (file, code, names = ["REFBACK"]) => ({
  file,
  code,
  line: 1,
  names,
});
const allowed = (file, code, count) => ({ file, code, count, reason: "why" });

test("the allowlist must describe the accepted violations exactly", () => {
  const cases = [
    {
      what: "an accepted violation",
      violations: [violation("a.ts", 'x === "REFBACK"')],
      allowlist: [allowed("a.ts", 'x === "REFBACK"', 1)],
      expected: [],
    },
    {
      what: "a new violation in a file that has an accepted one",
      violations: [
        violation("a.ts", 'x === "REFBACK"'),
        violation("a.ts", 'y === "PARTS"', ["PARTS"]),
      ],
      allowlist: [allowed("a.ts", 'x === "REFBACK"', 1)],
      expected: [
        'a.ts:1 `y === "PARTS"` — use isPartsType from metadata-utils/src/fieldHelpers.ts',
      ],
    },
    {
      what: "a violation naming several column types suggests each predicate",
      violations: [
        violation("a.ts", '["HEADING", "SECTION"].includes(x)', [
          "HEADING",
          "SECTION",
        ]),
      ],
      allowlist: [],
      expected: [
        'a.ts:1 `["HEADING", "SECTION"].includes(x)` — use isLayoutColumnType / isPlainHeadingType or isLayoutColumnType / isSectionType from metadata-utils/src/fieldHelpers.ts',
      ],
    },
    {
      what: "a lone layout name suggests both readings of that one name",
      violations: [
        violation("a.ts", 'c.columnType === "HEADING"', ["HEADING"]),
      ],
      allowlist: [],
      expected: [
        'a.ts:1 `c.columnType === "HEADING"` — use isLayoutColumnType / isPlainHeadingType from metadata-utils/src/fieldHelpers.ts',
      ],
    },
    {
      what: "one more occurrence than the allowlist accepts",
      violations: [
        violation("a.ts", 'x === "REFBACK"'),
        violation("a.ts", 'x === "REFBACK"'),
      ],
      allowlist: [allowed("a.ts", 'x === "REFBACK"', 1)],
      expected: ['a.ts: `x === "REFBACK"` occurs 2 times, allowlist accepts 1'],
    },
    {
      what: "an allowlist entry whose code is gone",
      violations: [],
      allowlist: [allowed("a.ts", 'x === "REFBACK"', 1)],
      expected: [
        'a.ts: stale allowlist entry, `x === "REFBACK"` no longer occurs',
      ],
    },
  ];

  for (const testCase of cases) {
    assert.deepEqual(
      reconcileWithAllowlist(testCase.violations, testCase.allowlist),
      testCase.expected,
      testCase.what
    );
  }
});

test("every allowlist entry says why the literal has to stay", () => {
  const unexplained = allowlist.filter(
    (entry) => !entry.reason || entry.reason.trim().length < 20
  );
  assert.deepEqual(unexplained, []);
});

test("no frontend package checks a column type by literal", () => {
  const problems = reconcileWithAllowlist(scanWorkspace(__dirname), allowlist);
  assert.deepEqual(problems, []);
});
