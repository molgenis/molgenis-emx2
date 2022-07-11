import { assert, test } from "vitest";
import { deepClone } from "./utils";

// Edit an assertion and save to see HMR in action

test("deepClone", () => {
  const input = {
    foo: "hello",
    bar: "world",
  };

  const output = deepClone(input);

  assert.deepEqual(output, input, "matches original");
});
