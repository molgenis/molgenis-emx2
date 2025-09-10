import { describe, expect, test, vi, beforeEach } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type { columnValue } from "../../../../metadata-utils/src/types";
import useForm from "../../../composables/useForm";
import { type Ref, ref } from "vue";
import consola from "consola";

describe("useForm", () => {
  const tableMetadata: Ref<ITableMetaData> = ref({
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
  });

  test("should return a list of required fields", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const { requiredFields } = useForm(tableMetadata, formValues, scrollTo);
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
    const scrollTo = vi.fn();
    const { emptyRequiredFields } = useForm(
      tableMetadata,
      formValues,
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
    const scrollTo = vi.fn();
    const { gotoNextRequiredField } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    gotoNextRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
    gotoNextRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });

  test("should go to the previous required field", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const { gotoPreviousRequiredField } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    gotoPreviousRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
    gotoPreviousRequiredField();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });

  test("setting a value on required field should update the message", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const { requiredMessage, emptyRequiredFields } = useForm(
      tableMetadata,
      formValues,
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
    const scrollTo = vi.fn();
    const { errorMessage, errorMap } = useForm(
      tableMetadata,
      formValues,
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
    const scrollTo = vi.fn();
    const { gotoNextError, errorMap } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    errorMap.value = {
      col2: "some error",
      col4: "some error",
    };
    gotoNextError();
    expect(scrollTo).toHaveBeenCalledWith("col2-form-field");
  });

  test("should go to the previous error", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const { gotoPreviousError, errorMap } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    errorMap.value = {
      col2: "some error",
      col4: "some error",
    };
    gotoPreviousError();
    expect(scrollTo).toHaveBeenCalledWith("col4-form-field");
  });

  test("should return empty list in case of table meta without columns", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [],
    });
    const { sections } = useForm(tableMetadata, formValues, scrollTo);
    expect(sections.value).toEqual([]);
  });

  test("should return a list of sections with error count", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [
        {
          columnType: "HEADING",
          id: "col1",
          label: "columns 1",
        },
        {
          columnType: "STRING",
          id: "col2",
          label: "columns 2",
          heading: "col1",
        },
        {
          columnType: "HEADING",
          id: "h2",
          label: "heading 2",
        },
        {
          columnType: "STRING",
          id: "col4",
          label: "columns 4",
          heading: "h2",
        },
      ],
    });

    const { sections, errorMap, gotoSection } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    errorMap.value = {
      col4: "error",
    };

    gotoSection("h2");

    expect(sections.value).toEqual([
      {
        label: "columns 1",
        id: "col1",
        isActive: false,
        errorCount: 0,
      },
      {
        label: "heading 2",
        id: "h2",
        isActive: true,
        errorCount: 1,
      },
    ]);
  });

  test("should add a heading at the start if the first col is not a header but the table has headings", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const tableMetadata: Ref<ITableMetaData> = ref({
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
        },
        {
          columnType: "HEADING",
          id: "h1",
          label: "heading 1",
        },
        {
          columnType: "STRING",
          id: "col3",
          label: "columns 3",
        },
      ],
    });
    const { sections } = useForm(tableMetadata, formValues, scrollTo);
    expect(sections.value[0]).toEqual({
      errorCount: 0,
      id: "h1",
      isActive: false,
      label: "heading 1",
    });
  });

  test("headings should be shown if at least one field is shown", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const scrollTo = vi.fn();
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [
        {
          columnType: "HEADING",
          id: "h1",
          label: "heading 1",
        },
        {
          columnType: "STRING",
          id: "col1",
          label: "columns 1",
        },
        {
          columnType: "HEADING",
          id: "h2",
          label: "heading 2",
        },
        {
          columnType: "STRING",
          id: "col2",
          label: "columns 2",
          visible: "col1",
        },
        {
          columnType: "HEADING",
          id: "h3",
          label: "heading 3",
          visible: "col2",
        },
        {
          columnType: "STRING",
          id: "col3",
          label: "columns 3",
        },
      ],
    });

    const { sections, visibleColumns, onBlurColumn } = useForm(
      tableMetadata,
      formValues,
      scrollTo
    );
    consola.level = 4;
    expect(sections.value[0]).toEqual({
      errorCount: 0,
      id: "h1",
      isActive: false,
      label: "heading 1",
    });
    expect(sections.value.length).toEqual(1);
    expect(visibleColumns.value.length).toEqual(2);

    //simulate update on col1
    formValues.value["col1"] = true;
    onBlurColumn(tableMetadata.value.columns[1]);
    expect(sections.value.length).toEqual(2);
    expect(visibleColumns.value.length).toEqual(4);

    //simulate update on col2
    formValues.value["col2"] = true;
    onBlurColumn(tableMetadata.value.columns[3]);
    expect(sections.value.length).toEqual(3);
    expect(visibleColumns.value.length).toEqual(6);

    //simulate update on col1
    //should invisible fields be emptied ???
    formValues.value["col1"] = false;
    onBlurColumn(tableMetadata.value.columns[1]);
    expect(sections.value.length).toEqual(1);
    expect(visibleColumns.value.length).toEqual(2);
  });
});
