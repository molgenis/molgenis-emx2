import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { ref } from "vue";
import FormField from "../../../../app/components/form/FormField.vue";
import type { UseForm } from "../../../../app/composables/useForm";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function formFor(column: IColumn, value: unknown) {
  return {
    values: ref({ [column.id]: value }),
    requiredFields: ref([]),
    rowKey: ref(null),
    metadata: ref({ schemaId: "s", id: "t" }),
    visibleColumnErrors: ref({}),
    onUpdateColumn: () => {},
    onBlurColumn: () => {},
  } as unknown as UseForm;
}

describe("FormField.vue", () => {
  it.each([
    ["ENUM", 'input[type="radio"]'],
    ["ENUM_ARRAY", 'input[type="checkbox"]'],
  ] as const)("renders the values of an %s column", (columnType, selector) => {
    const column: IColumn = {
      id: "status",
      label: "Status",
      columnType,
      values: ["open", "closed"],
    };

    const wrapper = mount(FormField, {
      props: { form: formFor(column, null), column },
    });

    expect(
      wrapper
        .findAll(selector)
        .map((input) => (input.element as HTMLInputElement).value)
    ).toEqual(["open", "closed"]);
  });
});
