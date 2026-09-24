const test = require("node:test");
const assert = require("node:assert");
const path = require("node:path");
const { pathToFileURL } = require("node:url");

const moduleUrl = pathToFileURL(
  path.join(__dirname, "..", "src", "tableModel.ts")
);

test("a column of a table marked for deletion is dropped", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const table = { name: "Person", oldName: "Person", drop: true };
  assert.strictEqual(isColumnOwnerDropped(table, { table: "Person" }), true);
});

test("a column of a table that is kept is not dropped", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const table = { name: "Person", oldName: "Person" };
  assert.strictEqual(isColumnOwnerDropped(table, { table: "Person" }), false);
});

test("a column of a subclass marked for deletion is dropped", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const table = {
    name: "Person",
    oldName: "Person",
    subclasses: [
      { name: "Student", oldName: "Student" },
      { name: "Employee", oldName: "Employee", drop: true },
    ],
  };
  assert.strictEqual(isColumnOwnerDropped(table, { table: "Employee" }), true);
  assert.strictEqual(isColumnOwnerDropped(table, { table: "Student" }), false);
});

test("a column of a renamed table follows that table's deletion state", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const renamed = { name: "Human", oldName: "Person", drop: true };
  assert.strictEqual(isColumnOwnerDropped(renamed, { table: "Human" }), true);
  assert.strictEqual(
    isColumnOwnerDropped(
      { name: "Human", oldName: "Person" },
      {
        table: "Human",
      }
    ),
    false
  );
});

test("a column of a renamed subclass follows that subclass's deletion state", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const table = {
    name: "Person",
    oldName: "Person",
    subclasses: [{ name: "Worker", oldName: "Employee", drop: true }],
  };
  assert.strictEqual(isColumnOwnerDropped(table, { table: "Worker" }), true);
});

test("a column whose owning table is not in this tree is not dropped", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const table = { name: "Person", oldName: "Person", drop: true };
  assert.strictEqual(
    isColumnOwnerDropped(table, { table: "Elsewhere" }),
    false
  );
});

test("a newly added column that names no table is not dropped", async () => {
  const { isColumnOwnerDropped } = await import(moduleUrl);
  const newTable = { name: "Person", subclasses: [{ name: "Student" }] };
  assert.strictEqual(isColumnOwnerDropped(newTable, {}), false);
});

test("renaming a table retags the columns it owns", async () => {
  const { applyTableRename } = await import(moduleUrl);
  const table = {
    name: "Human",
    oldName: "Person",
    columns: [
      { name: "firstName", table: "Person" },
      { name: "lastName", table: "Person" },
    ],
  };
  applyTableRename(table, "Person", "Human");
  assert.deepStrictEqual(
    table.columns.map((column) => column.table),
    ["Human", "Human"]
  );
});

test("renaming a subclass retags only the columns of that subclass", async () => {
  const { applyTableRename } = await import(moduleUrl);
  const rootTable = {
    name: "Person",
    oldName: "Person",
    columns: [
      { name: "firstName", table: "Person" },
      { name: "salary", table: "Employee" },
      { name: "grade", table: "Student" },
    ],
    subclasses: [
      { name: "Worker", oldName: "Employee" },
      { name: "Student", oldName: "Student" },
    ],
  };
  applyTableRename(rootTable, "Employee", "Worker");
  assert.deepStrictEqual(
    rootTable.columns.map((column) => column.table),
    ["Person", "Worker", "Student"]
  );
});

test("a table that keeps its name leaves its columns untouched", async () => {
  const { applyTableRename } = await import(moduleUrl);
  const table = {
    name: "Person",
    oldName: "Person",
    columns: [{ name: "firstName", table: "Person" }],
  };
  applyTableRename(table, "Person", "Person");
  assert.deepStrictEqual(
    table.columns.map((column) => column.table),
    ["Person"]
  );
});

test("the save payload of a renamed table keeps its columns", async () => {
  const { applyTableRename, toSaveTables } = await import(moduleUrl);
  const table = {
    name: "Person",
    oldName: "Person",
    columns: [{ name: "firstName", table: "Person" }],
  };
  const schema = { name: "myschema", tables: [table], ontologies: [] };
  table.name = "Human";
  applyTableRename(table, "Person", "Human");
  const payload = toSaveTables(schema);
  assert.deepStrictEqual(
    payload.map((saved) => saved.name),
    ["Human"]
  );
  assert.deepStrictEqual(
    payload[0].columns.map((column) => column.name),
    ["firstName"]
  );
});

test("the save payload of a renamed subclass keeps its columns", async () => {
  const { applyTableRename, toSaveTables } = await import(moduleUrl);
  const subclass = { name: "Employee", oldName: "Employee" };
  const rootTable = {
    name: "Person",
    oldName: "Person",
    columns: [
      { name: "firstName", table: "Person" },
      { name: "salary", table: "Employee" },
    ],
    subclasses: [subclass],
  };
  const schema = { name: "myschema", tables: [rootTable], ontologies: [] };
  subclass.name = "Worker";
  applyTableRename(rootTable, "Employee", "Worker");
  const payload = toSaveTables(schema);
  assert.deepStrictEqual(
    payload.map((saved) => saved.name),
    ["Person", "Worker"]
  );
  assert.deepStrictEqual(
    payload[0].columns.map((column) => column.name),
    ["firstName"]
  );
  assert.deepStrictEqual(
    payload[1].columns.map((column) => column.name),
    ["salary"]
  );
});

test("the save payload sends subclasses as tables of their own", async () => {
  const { toSaveTables } = await import(moduleUrl);
  const schema = {
    name: "myschema",
    tables: [
      {
        name: "Person",
        oldName: "Person",
        columns: [
          { name: "firstName", table: "Person" },
          { name: "salary", table: "Employee" },
        ],
        subclasses: [
          { name: "Employee", oldName: "Employee", inheritName: "Person" },
        ],
      },
    ],
    ontologies: [{ name: "Tags", oldName: "Tags", tableType: "ONTOLOGIES" }],
  };
  const payload = toSaveTables(schema);
  assert.deepStrictEqual(
    payload.map((saved) => saved.name),
    ["Person", "Employee", "Tags"]
  );
  assert.deepStrictEqual(
    payload[0].columns.map((column) => column.name),
    ["firstName"]
  );
  assert.deepStrictEqual(
    payload[1].columns.map((column) => column.name),
    ["salary"]
  );
  assert.strictEqual(payload[0].subclasses, undefined);
});
