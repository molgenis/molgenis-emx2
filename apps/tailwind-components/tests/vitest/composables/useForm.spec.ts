import { describe, expect, test, vi } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type {
  columnId,
  columnValue,
} from "../../../../metadata-utils/src/types";

describe("useForm", () => {
  const tableMetadata: ITableMetaData = {
    id: "vi test table metadata",
    label: "vi test table metadata",
    tableType: "some table type",
    columns: [
      {
        columnType: "STRING",
        id: "col1",
        label: "columns 1",
      },
      {
        columnType: "STRING",
        id: "col2",
        label: "columns 2",
        required: true,
      },
      {
        columnType: "STRING",
        id: "col3",
        label: "columns 3",
      },
      {
        columnType: "STRING",
        id: "col4",
        label: "columns 4",
        required: true,
      },
    ],
  };

  test("should return a list of required fields", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { requiredFields } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    expect(requiredFields.value).toEqual([
      {
        columnType: "STRING",
        id: "col2",
        label: "columns 2",
        required: true,
      },
      {
        columnType: "STRING",
        id: "col4",
        label: "columns 4",
        required: true,
      },
    ]);
  });

  test("should return a list of empty required fields", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { emptyRequiredFields } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    expect(emptyRequiredFields.value).toEqual([
      {
        columnType: "STRING",
        id: "col2",
        label: "columns 2",
        required: true,
      },
      {
        columnType: "STRING",
        id: "col4",
        label: "columns 4",
        required: true,
      },
    ]);
  });

  test("should go to the next required field", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { gotoNextRequiredField } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    gotoNextRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
    gotoNextRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });

  test("should go to the previous required field", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { gotoPreviousRequiredField } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    gotoPreviousRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
    gotoPreviousRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });

  test("setting a value on required field should update the message", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { requiredMessage, emptyRequiredFields } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    expect(requiredMessage.value).toBe("2/2 required fields left");

    // setting a value removes the field from the required list
    formValues.value["col2"] = "some value";
    expect(emptyRequiredFields.value).toEqual([
      {
        columnType: "STRING",
        id: "col4",
        label: "columns 4",
        required: true,
      },
    ]);
    expect(requiredMessage.value).toBe("1/2 required field left");
  });

  test("setting an error should update the message", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({});
    const scrollTo = vi.fn();
    const { errorMessage } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    expect(errorMessage.value).toBe("");

    errorMap.value["col2"] = "some error";
    expect(errorMessage.value).toBe(
      "1 field requires attention before you can save this cohort"
    );
  });

  test("should go to the next error", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({
      col2: "some error",
      col4: "some error",
    });
    const scrollTo = vi.fn();
    const { gotoNextError } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    gotoNextError();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
  });

  test("should go to the previous error", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const errorMap = ref<Record<columnId, string>>({
      col2: "some error",
      col4: "some error",
    });
    const scrollTo = vi.fn();
    const { gotoPreviousError } = useForm(
      tableMetadata,
      formValues,
      errorMap,
      scrollTo
    );
    gotoPreviousError();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });
});
