import { mount } from "@vue/test-utils";
import { beforeEach, describe, expect, test } from "vitest";
import type {
  ColumnType,
  columnValueObject,
  IColumn,
  ITableMetaData,
} from "../../../../../../metadata-utils/src/types";
import InputRefListItem from "../../../../../app/components/input/ref/ListItem.vue";

function column(id: string, columnType: ColumnType, label: string): IColumn {
  return { id, label, columnType };
}

// Two SECTIONs, because DetailView hides the menu below two of them anyway.
// Without them `showMenu` could not be seen to do anything.
const metadata: ITableMetaData = {
  id: "Pet",
  schemaId: "pet store",
  name: "Pet",
  label: "Pet",
  tableType: "DATA",
  columns: [
    column("about", "SECTION", "About"),
    column("name", "STRING", "Name"),
    column("size", "HEADING", "Size"),
    column("weight", "DECIMAL", "Weight"),
    column("care", "SECTION", "Care"),
    column("diet", "STRING", "Diet"),
  ],
};

const refData: columnValueObject = {
  name: "spike",
  weight: 15.7,
  diet: "insects",
};

describe("InputRefListItem", () => {
  let wrapper: ReturnType<typeof mount>;

  beforeEach(() => {
    wrapper = mount(InputRefListItem, {
      props: { refData, refMetadata: metadata, refLabel: "name" },
    });
  });

  test("shows the record's fields with their labels and values", () => {
    const text = wrapper.text();
    for (const shown of ["Name", "spike", "Weight", "15.7", "Diet", "insects"])
      expect(text).toContain(shown);
  });

  test("renders no section menu", () => {
    expect(wrapper.find('nav[aria-label="Section navigation"]').exists()).toBe(
      false
    );
  });

  test("renders no field filter", () => {
    expect(wrapper.find('input[type="search"]').exists()).toBe(false);
  });
});
