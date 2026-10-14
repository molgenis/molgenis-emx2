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
    ["ENUM", undefined],
    ["ENUM_ARRAY", "true"],
  ] as const)(
    "renders the values of an %s column",
    (columnType, multiselectable) => {
      const column: IColumn = {
        id: "status",
        label: "Status",
        columnType,
        values: ["open", "closed"],
      };

      const wrapper = mount(FormField, {
        props: { form: formFor(column, null), column },
      });

      expect(wrapper.findAll('li[role="option"]').map((o) => o.text())).toEqual(
        ["open", "closed"]
      );
      expect(
        wrapper.find('ul[role="listbox"]').attributes("aria-multiselectable")
      ).toBe(multiselectable);
    }
  );
});
