import test from "node:test";
import assert from "node:assert/strict";
import { fieldTypes } from "metadata-utils";
import columnTypes from "./columnTypes.js";

test("column type dropdown offers exactly the column types the metadata model knows", () => {
  const knownColumnTypes = fieldTypes();
  const missingFromDropdown = knownColumnTypes.filter(
    (columnType) => !columnTypes.includes(columnType)
  );
  const unknownInDropdown = columnTypes.filter(
    (columnType) => !knownColumnTypes.includes(columnType)
  );
  assert.deepEqual(
    { missingFromDropdown, unknownInDropdown },
    { missingFromDropdown: [], unknownInDropdown: [] }
  );
});
