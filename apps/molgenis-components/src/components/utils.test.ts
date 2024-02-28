import { describe, assert, test, expect, vi, it } from "vitest";
import constants from "./constants";
import {
  applyJsTemplate,
  convertRowToPrimaryKey,
  deepClone,
  deepEqual,
  flattenObject,
  getBigIntError,
  isNumericKey,
  isRefType,
} from "./utils";
import { contactsMetadata, resourcesMetadata } from "./mockDatasets";

vi.mock("../client/client", () => {
  // For use with convertRowToPrimaryKey
  return {
    default: {
      newClient: () => ({
        fetchTableMetaData: (tableId: string) => {
          if (tableId === "Resources") return resourcesMetadata;
          else if (tableId === "Contacts") return contactsMetadata;
          else return {};
        },
      }),
    },
  };
});

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

describe("convertRowToPrimaryKey", () => {
  test("it should convert a IRow object to only its primaryKey", async () => {
    const row = {
      resource: {
        id: "TEST",
        name: "TEST Study",
        description: "TEST description",
        mg_tableclass: "Catalogue.Cohorts",
      },
      firstName: "Jan",
      lastName: "Modal",
      email: "Jan@modal.nl",
      orcid: "0000-0000-0000-0000",
      photo: {},
      mg_draft: false,
    };
    const expectedPrimaryKey = {
      resource: {
        id: "TEST",
      },
      firstName: "Jan",
      lastName: "Modal",
    };
    expect(await convertRowToPrimaryKey(row, "Contacts", "")).toStrictEqual(
      expectedPrimaryKey
    );
  });

  test("it should fail if there is not metadata found", async () => {
    try {
      await convertRowToPrimaryKey({}, "Unknown table", "");
    } catch (error) {
      expect((error as Error).message).toBe("Empty columns in metadata");
    }
  });
});

describe("getBigIntError", () => {
  const BIG_INT_ERROR = `Invalid value: must be value from -9223372036854775807 to 9223372036854775807`;

  test("it should return undefined for a valid positive long", () => {
    expect(getBigIntError("9223372036854775807")).toBeUndefined();
  });

  test("it should return undefined for a valid negative long", () => {
    expect(getBigIntError("-9223372036854775807")).toBeUndefined();
  });

  test("it should return an error string for a too large long", () => {
    expect(getBigIntError("9223372036854775808")).toEqual(BIG_INT_ERROR);
  });

  test("it should return an error string for a too small long", () => {
    expect(getBigIntError("-9223372036854775808")).toEqual(BIG_INT_ERROR);
  });

  test("it should return an error for invalid input", () => {
    expect(getBigIntError("randomtext")).toEqual(BIG_INT_ERROR);
  });

  test("it should return an error for empty inputs", () => {
    expect(getBigIntError("")).toEqual(BIG_INT_ERROR);
  });

  test("it should return an error for only a minus", () => {
    expect(getBigIntError("-")).toEqual(BIG_INT_ERROR);
  });
});

describe("applyJsTemplate", () => {
  test("it should return the label according to the template", () => {
    const object = { id: "someid", name: "naam", otherField: "bla" };
    const labelTemplate = "${otherField}";
    const result = applyJsTemplate(object, labelTemplate);
    expect(result).toEqual("bla");
  });

  test("it should return the name if the label is empty and there is no primaryKey", () => {
    const object = { id: "someid", primaryKey: { id: "primKey" } };
    const labelTemplate = "${nonExistantField}";
    const result = applyJsTemplate(object, labelTemplate);
    expect(result).toEqual(" primKey");
  });

  test("it should return the name if the label is empty and there is no primaryKey", () => {
    const object = { id: "someid", name: "naam" };
    const labelTemplate = "${nonExistantField}";
    const result = applyJsTemplate(object, labelTemplate);
    expect(result).toEqual("naam");
  });

  test("it should return the id if the label is empty and there is no primaryKey or name", () => {
    const object = { id: "someid" };
    const labelTemplate = "${nonExistantField}";
    const result = applyJsTemplate(object, labelTemplate);
    expect(result).toEqual("someid");
  });
});
