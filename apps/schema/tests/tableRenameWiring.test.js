const test = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");

const componentsDir = path.join(__dirname, "..", "src", "components");

function componentSources() {
  return fs
    .readdirSync(componentsDir)
    .filter((file) => file.endsWith(".vue"))
    .map((file) => ({
      file,
      source: fs.readFileSync(path.join(componentsDir, file), "utf8"),
    }));
}

function tableEditModalMounts(source) {
  return source.match(/<TableEditModal\b[\s\S]*?\/>/g) || [];
}

test("every table edit modal that edits an existing table applies the rename to the model", () => {
  const editSites = componentSources().flatMap(({ file, source }) =>
    tableEditModalMounts(source)
      .filter((mount) => /\s:modelValue=/.test(mount))
      .map((mount) => ({ file, source, mount }))
  );
  assert.deepStrictEqual(
    editSites.map((site) => site.file),
    ["OntologyView.vue", "TableView.vue", "TableView.vue"]
  );
  editSites.forEach(({ file, source }) => {
    assert.ok(
      /applyTableRename\(/.test(source),
      `${file} edits an existing table without applying the rename to the model`
    );
  });
});

test("no table edit modal writes back through v-model, which would bypass the rename", () => {
  const boundWithVModel = componentSources().flatMap(({ file, source }) =>
    tableEditModalMounts(source)
      .filter((mount) => /v-model=/.test(mount))
      .map(() => file)
  );
  assert.deepStrictEqual(boundWithVModel, []);
});
