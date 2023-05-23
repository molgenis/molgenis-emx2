import { describe, assert, test } from "vitest";
import constants from "./constants";
import { deepClone, deepEqual, flattenObject, isNumericKey, isRefType } from "./utils";

const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_MINUS, CODE_DELETE } = constants;

describe("isRefType", () => {
  test("it should return true for REF, REF_ARRAY, REFBACK, ONTOLOGY, and ONTOLOGY_ARRAY types", () => {
    assert.isTrue(isRefType("REF"));
    assert.isTrue(isRefType("REF_ARRAY"));
    assert.isTrue(isRefType("REFBACK"));
    assert.isTrue(isRefType("ONTOLOGY"));
    assert.isTrue(isRefType("ONTOLOGY_ARRAY"));
  });

  test("it should return false for other types", () => {
    assert.isFalse(isRefType("SOME_OTHER_TYPE"));
  });
});

describe("isNumericKey", () => {
  test("code is CODE_0 (48)", () => {
    const keyboardEvent = { which: CODE_0 } as KeyboardEvent;
    assert.isTrue(isNumericKey(keyboardEvent));
  });

  test("code is between CODE_0 (48) and CODE_9 (57)", () => {
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

  test("code is not numerical or input modification", () => {
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
describe("deepClone", () => {
  test("it should make a clone of the input", () => {
    const input = {
      foo: "hello",
      bar: "world",
    };

    const output = deepClone(input);

    assert.deepEqual(output, input, "matches original");
  });
});

describe("deepEqual", () => {
  test("it should return true if 2 objects are equal", () => {
    const object1 = { id: "someId", some: "property" };
    const object2 = { id: "someId", some: "property" };
    assert.isTrue(deepEqual(object1, object2));
  });

  test("it should return true if 2 complex objects are equal", () => {
    const object1 = {
      id: "someId",
      some: "property",
      innerObject: { another: "prop" },
    };
    const object2 = {
      id: "someId",
      some: "property",
      innerObject: { another: "prop" },
    };
    assert.isTrue(deepEqual(object1, object2));
  });

  test("it should return false if 2 complex objects are not  equal", () => {
    const object1 = {
      id: "someId",
      some: "property",
      innerObject: { another: "prop" },
    };
    const object2 = {
      id: "someId",
      some: "property",
      innerObject: { another: "prop", additional: "but it has more" },
    };
    assert.isFalse(deepEqual(object1, object2));
  });
});
