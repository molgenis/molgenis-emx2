import { describe, assert, test } from "vitest";
import constants from "./constants";
import { deepClone, flattenObject, isNumericKey } from "./utils";

const {
  CODE_0,
  CODE_9,
  CODE_BACKSPACE,
  CODE_MINUS,
  CODE_DELETE,
  MIN_LONG,
  MAX_LONG,
} = constants;

describe("isNumericKey", () => {
  test("code is CODE_0 (48)", () => {
    const keyboardEvent = { which: CODE_0 } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is between CODE_0 and CODE_9", () => {
    const keyboardEvent = { which: 50 } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is CODE_9 (57)", () => {
    const keyboardEvent = { which: CODE_9 } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is CODE_BACKSPACE (8)", () => {
    const keyboardEvent = { which: CODE_BACKSPACE } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is CODE_DELETE (46)", () => {
    const keyboardEvent = { which: CODE_DELETE } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is not numerical of input modification", () => {
    const keyboardEvent = { which: CODE_MINUS } as KeyboardEvent;
    assert.isFalse(isNumericKey(keyboardEvent));
  });
});

describe("flattenObject", () => {
  test(`return value if it is not an object. 
        This can only happen is the function is called from js`, () => {
    const notAnObject = "foo" as unknown as Record<string, any>;
    const result = flattenObject(notAnObject);
    assert.deepEqual("foo", result);
  });

  test("it should flatten an object into a string", () => {
    const input = { foo: null, bar: "first", buz: 2, bool: true };
    const result = flattenObject(input);
    const expectedResult = " first 2 true";
    assert.deepEqual(expectedResult, result);
  });

  test("it should recursively flatten inner objects", () => {
    const input = { foo: { first: 123 }, buz: 2 };
    const result = flattenObject(input);
    const expectedResult = " 123 2";
    assert.deepEqual(expectedResult, result);
  });
});

test("deepClone", () => {
  const input = {
    foo: "hello",
    bar: "world",
  };

  const output = deepClone(input);

  assert.deepEqual(output, input, "matches original");
});
