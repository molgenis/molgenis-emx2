const test = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");

const utilsSource = fs.readFileSync(
  path.join(__dirname, "..", "src", "utils.ts"),
  "utf8"
);

function selectedFieldPaths(source) {
  const query = source.slice(
    source.indexOf("`", source.indexOf("export const schemaQuery = gql")) + 1,
    source.indexOf("`;")
  );
  const parents = [];
  const paths = [];
  query.split("\n").forEach((rawLine) => {
    const line = rawLine.trim();
    const blockStart = line.match(/^([A-Za-z_][A-Za-z0-9_]*)[^{]*\{$/);
    if (line === "}") {
      parents.pop();
    } else if (blockStart) {
      parents.push(blockStart[1]);
    } else if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(line)) {
      paths.push(parents.concat(line).join("."));
    }
  });
  return paths;
}

test("schema editor query selects the inherited parent's schema name", () => {
  const paths = selectedFieldPaths(utilsSource);
  assert.ok(
    paths.includes("_schema.tables.inheritNames"),
    `expected _schema.tables.inheritNames in ${JSON.stringify(paths)}`
  );
  assert.ok(
    paths.includes("_schema.tables.inheritSchemaName"),
    `expected _schema.tables.inheritSchemaName in ${JSON.stringify(paths)}`
  );
});
