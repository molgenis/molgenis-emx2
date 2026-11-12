const test = require("node:test");
const assert = require("node:assert");
const path = require("node:path");
const { pathToFileURL } = require("node:url");

const moduleUrl = pathToFileURL(
  path.join(__dirname, "..", "src", "inheritSchema.ts")
);

test("selecting the current schema as refSchema yields no inheritSchemaName", async () => {
  const { toInheritSchemaName } = await import(moduleUrl);
  assert.strictEqual(toInheritSchemaName("myschema", "myschema"), undefined);
});

test("selecting another schema as refSchema yields that schema name", async () => {
  const { toInheritSchemaName } = await import(moduleUrl);
  assert.strictEqual(
    toInheritSchemaName("otherschema", "myschema"),
    "otherschema"
  );
});

test("clearing refSchema yields no inheritSchemaName", async () => {
  const { toInheritSchemaName } = await import(moduleUrl);
  assert.strictEqual(toInheritSchemaName(null, "myschema"), undefined);
  assert.strictEqual(toInheritSchemaName(undefined, "myschema"), undefined);
});

test("parent table options list data tables and their subclasses", async () => {
  const { extendableTableNames } = await import(moduleUrl);
  const schema = {
    tables: [
      { name: "parent", subclasses: [{ name: "child" }] },
      { name: "standalone" },
      { name: "tags", tableType: "ONTOLOGIES" },
    ],
  };
  assert.deepStrictEqual(extendableTableNames(schema), [
    "parent",
    "child",
    "standalone",
  ]);
});

test("parent table options exclude tables marked for deletion", async () => {
  const { extendableTableNames } = await import(moduleUrl);
  const schema = { tables: [{ name: "doomed", drop: true }, { name: "kept" }] };
  assert.deepStrictEqual(extendableTableNames(schema), ["kept"]);
});

test("parent table options of an unloaded schema are empty", async () => {
  const { extendableTableNames } = await import(moduleUrl);
  assert.deepStrictEqual(extendableTableNames(undefined), []);
});

test("a new table extending a table of this schema is added to that root table", async () => {
  const { findLocalRootTable } = await import(moduleUrl);
  const schema = {
    name: "myschema",
    tables: [{ name: "other" }, { name: "parent" }],
  };
  const rootTable = findLocalRootTable(schema, {
    name: "newChild",
    inheritName: "parent",
  });
  assert.strictEqual(rootTable, schema.tables[1]);
});

test("a new table extending a subclass is added to that subclass's root table", async () => {
  const { findLocalRootTable } = await import(moduleUrl);
  const schema = {
    name: "myschema",
    tables: [{ name: "parent", subclasses: [{ name: "sub" }] }],
  };
  const rootTable = findLocalRootTable(schema, {
    name: "newChild",
    inheritName: "sub",
  });
  assert.strictEqual(rootTable, schema.tables[0]);
});

test("a new table is never attached to a subclass listed alongside its own root table", async () => {
  const { findLocalRootTable } = await import(moduleUrl);
  const subclass = { name: "sub", inheritName: "parent" };
  const schema = {
    name: "myschema",
    tables: [subclass, { name: "parent", subclasses: [subclass] }],
  };
  const rootTable = findLocalRootTable(schema, {
    name: "newChild",
    inheritName: "sub",
  });
  assert.strictEqual(rootTable, schema.tables[1]);
  assert.notStrictEqual(rootTable, subclass);
});

test("a cross schema child is never attached to a same named table of this schema", async () => {
  const { findLocalRootTable } = await import(moduleUrl);
  const schema = { name: "myschema", tables: [{ name: "parent" }] };
  const rootTable = findLocalRootTable(schema, {
    name: "newChild",
    inheritName: "parent",
    inheritSchemaName: "otherschema",
  });
  assert.strictEqual(rootTable, undefined);
});

test("a new table without a parent is not attached to any root table", async () => {
  const { findLocalRootTable } = await import(moduleUrl);
  assert.strictEqual(
    findLocalRootTable(
      { name: "myschema", tables: [{ name: "parent" }] },
      { name: "loner" }
    ),
    undefined
  );
});

test("a self named qualifier still nests the table under its parent", async () => {
  const { findLocalRootTable, isRootTable, isCrossSchemaSubclass } =
    await import(moduleUrl);
  const selfQualified = {
    name: "sub",
    inheritName: "parent",
    inheritSchemaName: "myschema",
  };
  assert.strictEqual(isCrossSchemaSubclass(selfQualified, "myschema"), false);
  assert.strictEqual(isRootTable(selfQualified, "myschema"), false);
  const schema = { name: "myschema", tables: [{ name: "parent" }] };
  assert.strictEqual(
    findLocalRootTable(schema, selfQualified),
    schema.tables[0]
  );
});

test("a genuine cross schema child is a root table of the schema it lives in", async () => {
  const { isRootTable, isCrossSchemaSubclass } = await import(moduleUrl);
  const child = {
    name: "child",
    inheritName: "parent",
    inheritSchemaName: "otherschema",
  };
  assert.strictEqual(isCrossSchemaSubclass(child, "myschema"), true);
  assert.strictEqual(isRootTable(child, "myschema"), true);
});

test("a same schema subclass is not a root table", async () => {
  const { isRootTable } = await import(moduleUrl);
  assert.strictEqual(
    isRootTable({ name: "sub", inheritName: "parent" }, "myschema"),
    false
  );
});

test("the extends options of a subclass are the root table and its sibling subclasses", async () => {
  const { parentTableOptions } = await import(moduleUrl);
  const root = {
    name: "A",
    subclasses: [
      { name: "B", inheritName: "A" },
      { name: "C", inheritName: "B" },
    ],
  };
  assert.deepStrictEqual(parentTableOptions(root, "C"), ["A", "B"]);
});

test("editing a grandchild keeps its own parent instead of the root table", async () => {
  const { parentTableOptions, resolveInheritName } = await import(moduleUrl);
  const root = {
    name: "A",
    subclasses: [
      { name: "B", inheritName: "A" },
      { name: "C", inheritName: "B" },
    ],
  };
  const options = parentTableOptions(root, "C");
  assert.strictEqual(options[0], "A");
  assert.strictEqual(resolveInheritName("B", options), "B");
});

test("editing a direct child keeps the root table as its parent", async () => {
  const { parentTableOptions, resolveInheritName } = await import(moduleUrl);
  const root = {
    name: "A",
    subclasses: [
      { name: "B", inheritName: "A" },
      { name: "C", inheritName: "B" },
    ],
  };
  assert.strictEqual(
    resolveInheritName("A", parentTableOptions(root, "B")),
    "A"
  );
});

test("a new subclass extends the root table by default", async () => {
  const { parentTableOptions, resolveInheritName } = await import(moduleUrl);
  const root = { name: "A", subclasses: [{ name: "B", inheritName: "A" }] };
  assert.strictEqual(
    resolveInheritName(undefined, parentTableOptions(root, undefined)),
    "A"
  );
});

test("a root table without subclasses only offers itself as parent", async () => {
  const { parentTableOptions, resolveInheritName } = await import(moduleUrl);
  const root = { name: "A" };
  assert.deepStrictEqual(parentTableOptions(root, undefined), ["A"]);
  assert.strictEqual(resolveInheritName(undefined, ["A"]), "A");
});

test("a subclass of a cross schema child extends that child, not its external parent", async () => {
  const { parentTableOptions, resolveInheritName } = await import(moduleUrl);
  const crossSchemaRoot = {
    name: "B",
    inheritName: "A",
    inheritSchemaName: "otherschema",
    subclasses: [{ name: "C", inheritName: "B" }],
  };
  const options = parentTableOptions(crossSchemaRoot, "C");
  assert.deepStrictEqual(options, ["B"]);
  assert.strictEqual(resolveInheritName("B", options), "B");
});

test("a parent that is no longer offered falls back to the root table", async () => {
  const { resolveInheritName } = await import(moduleUrl);
  assert.strictEqual(resolveInheritName("removedSubclass", ["A", "B"]), "A");
});

test("parent table options list every table once when subclasses are also listed at top level", async () => {
  const { extendableTableNames } = await import(moduleUrl);
  const subclass = { name: "sub", inheritName: "parent" };
  const schema = {
    tables: [subclass, { name: "parent", subclasses: [subclass] }],
  };
  assert.deepStrictEqual(extendableTableNames(schema), ["sub", "parent"]);
});
