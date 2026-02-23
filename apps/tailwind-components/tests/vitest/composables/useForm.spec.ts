import { describe, expect, test } from "vitest";
import { type Ref, ref } from "vue";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type { columnValue } from "../../../../metadata-utils/src/types";
import useForm from "../../../app/composables/useForm";

describe("useForm", () => {
  const tableMetadata: Ref<ITableMetaData> = ref({
    id: "vi test table metadata",
    name: "vi test table metadata",
    schemaId: "vi test table metadata",
    label: "vi test table metadata label",
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
      {
        columnType: "AUTO_ID",
        id: "col5",
        label: "columns 5",
        required: "4 + 3 < 5",
      },
      {
        columnType: "BOOL",
        id: "col6",
        label: "columns 6",
        required: "true",
      },
    ],
  });

  test("should return a list of required fields", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const { requiredFields } = useForm(tableMetadata, formValues);
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
      {
        columnType: "BOOL",
        id: "col6",
        label: "columns 6",
        required: "true",
      },
    ]);
  });

  test("should return a list of empty required fields", () => {
    const formValues = ref<Record<string, columnValue>>({
      // non empty required bool field
      col6: false,
    });
    const { emptyRequiredFields } = useForm(tableMetadata, formValues);
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
    const formValues = ref<Record<string, columnValue>>({
      // non empty required bool field
      col6: false,
    });
    const { gotoNextRequiredField, lastScrollTo } = useForm(
      tableMetadata,
      formValues
    );
    gotoNextRequiredField();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col2-form-field"
    );
    gotoNextRequiredField();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col4-form-field"
    );
  });

  test("should go to the previous required field", () => {
    const formValues = ref<Record<string, columnValue>>({
      // non empty required bool field
      col6: false,
    });
    const { gotoPreviousRequiredField, lastScrollTo } = useForm(
      tableMetadata,
      formValues
    );
    gotoPreviousRequiredField();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col2-form-field"
    );
    gotoPreviousRequiredField();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col4-form-field"
    );
  });

  test("setting a value on required field should update the message", () => {
    const formValues = ref<Record<string, columnValue>>({
      // non empty required bool field
      col6: false,
    });
    const { requiredMessage, emptyRequiredFields } = useForm(
      tableMetadata,
      formValues
    );
    expect(requiredMessage.value).toBe("2/3 required fields left");

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
    expect(requiredMessage.value).toBe("1/3 required field left");
  });

  test("should go to the next error", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const { gotoNextError, lastScrollTo, validateAllColumns } = useForm(
      tableMetadata,
      formValues
    );
    validateAllColumns();
    gotoNextError();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col2-form-field"
    );
  });

  test("should go to the previous error", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const { gotoPreviousError, lastScrollTo, validateAllColumns } = useForm(
      tableMetadata,
      formValues
    );
    validateAllColumns();
    gotoPreviousError();
    expect(lastScrollTo.value).equals(
      "vi test table metadata-vi test table metadata-col6-form-field"
    );
  });

  test("should return empty list in case of table meta without columns", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      name: "vi test table metadata",
      schemaId: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [],
    });
    const { sections } = useForm(tableMetadata, formValues);
    expect(sections.value).toEqual([]);
  });

  test("should return a list of sections with error count", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      name: "vi test table metadata",
      schemaId: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [
        {
          columnType: "SECTION",
          id: "mg_top_of_form",
          label: "_top",
          section: "mg_top_of_form",
        },
        {
          columnType: "HEADING",
          id: "col1",
          label: "columns 1",
          section: "mg_top_of_form",
        },
        {
          columnType: "STRING",
          id: "col2",
          label: "columns 2",
          heading: "col1",
          section: "mg_top_of_form",
        },
        {
          columnType: "HEADING",
          id: "h2",
          label: "heading 2",
          section: "mg_top_of_form",
        },
        {
          columnType: "STRING",
          id: "col4",
          label: "columns 4",
          heading: "h2",
          section: "mg_top_of_form",
          required: true,
        },
      ],
    });

    const { sections, gotoSection, validateAllColumns } = useForm(
      tableMetadata,
      formValues
    );

    validateAllColumns();

    expect(sections.value.length).toEqual(1);
    expect(sections.value[0]).toEqual({
      errorCount: 1,
      headers: [
        {
          errorCount: 0,
          id: "col1",
          isActive: false,
          isVisible: true,
          label: "columns 1",
          type: "HEADING",
        },
        {
          errorCount: 1,
          id: "h2",
          isActive: false,
          isVisible: true,
          label: "heading 2",
          type: "HEADING",
        },
      ],
      id: "mg_top_of_form",
      isActive: false,
      isVisible: true,
      label: "_top",
      type: "SECTION",
    });
  });

  test("headings should be shown if at least one field is shown", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      name: "vi test table metadata",
      schemaId: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [
        {
          columnType: "SECTION",
          id: "main",
          label: "we always need a section",
          section: "main",
        },
        {
          columnType: "HEADING",
          id: "h1",
          label: "heading 1",
          section: "main",
        },
        {
          columnType: "STRING",
          id: "col1",
          label: "columns 1",
          section: "main",
          heading: "h1",
        },
        {
          columnType: "HEADING",
          id: "h2",
          label: "heading 2",
          section: "main",
        },
        {
          columnType: "STRING",
          id: "col2",
          label: "columns 2",
          visible: "col1",
          section: "main",
          heading: "h2",
        },
        {
          columnType: "HEADING",
          id: "h3",
          label: "heading 3",
          visible: "col2",
          section: "main",
        },
        {
          columnType: "STRING",
          id: "col3",
          label: "columns 3",
          section: "main",
          heading: "h3",
        },
      ],
    });

    const { sections } = useForm(tableMetadata, formValues);

    expect(sections.value[0].headers[1].isVisible).toBe(false);

    // make col1 visible, should make h1 visible
    formValues.value["col1"] = true;

    expect(sections.value[0].headers[1].isVisible).toBe(true);
  });

  test("section prev, current and next section", () => {
    const formValues = ref<Record<string, columnValue>>({});
    const tableMetadata: Ref<ITableMetaData> = ref({
      id: "vi test table metadata",
      name: "vi test table metadata",
      schemaId: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [
        {
          columnType: "SECTION",
          id: "main",
          label: "we always need a section",
          section: "main",
        },
        {
          columnType: "HEADING",
          id: "h1",
          label: "heading 1",
          section: "main",
        },
        {
          columnType: "STRING",
          id: "col1",
          label: "columns 1",
          section: "main",
          heading: "h1",
        },
        {
          columnType: "SECTION",
          id: "next",
          label: "next",
        },
        {
          columnType: "HEADING",
          id: "h2",
          label: "heading 2",
          section: "next",
        },
        {
          columnType: "STRING",
          id: "col2",
          label: "columns 2",
          section: "next",
          heading: "h2",
        },
        {
          columnType: "HEADING",
          id: "h3",
          label: "heading 3",
          section: "main",
        },
        {
          columnType: "STRING",
          id: "col3",
          label: "columns 3",
          section: "next",
          heading: "h3",
        },
      ],
    });
    const { currentSection, previousSection, nextSection, onViewColumn } =
      useForm(tableMetadata, formValues);

    onViewColumn({
      columnType: "STRING",
      id: "col1",
      label: "columns 1",
      section: "main",
      heading: "h1",
    }); // to activate the first section
    expect(previousSection.value).toEqual(null);
    expect(currentSection.value).toEqual("main");
    expect(nextSection.value?.id).toEqual("next");
  });

  describe("validateKeyColumns", () => {
    const meta: Ref<ITableMetaData> = ref({
      id: "table id",
      name: "table name",
      schemaId: "table schema id",
      label: "table label",
      tableType: "some table type",
      columns: [
        {
          id: "my_key",
          label: "My key",
          columnType: "STRING",
          key: 1,
          required: true,
        },
        {
          id: "other_column",
          label: "other column",
          columnType: "STRING",
          required: true,
        },
      ],
    });

    test("should only evaluate key columns", () => {
      const formValues = ref({
        my_key: "value",
      });
      const { validateKeyColumns, visibleColumnErrors } = useForm(
        meta,
        formValues
      );
      validateKeyColumns();
      expect(Object.keys(visibleColumnErrors.value).length).toBe(0);
    });

    test("should set error when a key column is empty", () => {
      const formValues = ref({});
      const { validateKeyColumns, visibleColumnErrors } = useForm(
        meta,
        formValues
      );
      validateKeyColumns();
      expect(visibleColumnErrors.value["my_key"]).toBe("My key is required");
    });
  });

  describe("requiredMap", () => {
    test("should correctly compute requiredMap", () => {
      const formValues = ref<Record<string, columnValue>>({});
      const { requiredMap } = useForm(tableMetadata, formValues);
      expect(requiredMap["col1"].value).toBe(false);
      expect(requiredMap["col2"].value).toBe(true);
      expect(requiredMap["col3"].value).toBe(false);
      expect(requiredMap["col4"].value).toBe(true);
      expect(requiredMap["col5"].value).toBe(false);
      expect(requiredMap["col6"].value).toBe(true);
    });
  });
});
