import { describe, expect, test, vi } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src";
import type { columnId } from "../../../../metadata-utils/src/types";

describe("useSections", () => {
  let errorMap = ref<Record<columnId, string>>({});
  let activeChapterId = ref<columnId>("col1");

  test("should return empty list in case of table meta without columns", () => {
    const tableMetadata: ITableMetaData = {
      id: "vi test table metadata",
      label: "vi test table metadata",
      tableType: "some table type",
      columns: [],
    };

    const sections = useSections(tableMetadata, activeChapterId, errorMap);

    expect(sections.value).toEqual([]);
  });

  test("should return a list of sections with error count", () => {
    const tableMetadata: ITableMetaData = {
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
        },
      ],
    };

    errorMap = ref<Record<columnId, string>>({
      col4: "error",
    });

    activeChapterId = ref<columnId>("h2");

    const sections = useSections(tableMetadata, activeChapterId, errorMap);

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
    };

    errorMap = ref<Record<columnId, string>>({});
    activeChapterId = ref<columnId>("");

    const sections = useSections(tableMetadata, activeChapterId, errorMap);

    expect(sections.value[0]).toEqual({
      errorCount: 0,
      label: "_top",
      id: "_scroll_to_top",
      isActive: true,
    });
  });
});
