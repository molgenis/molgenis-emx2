import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Field from "../../../app/components/Field.vue";

describe("Field.vue with an enum column", () => {
  it("offers Clear unless the column is required", () => {
    const props = {
      id: "sex",
      type: "ENUM" as const,
      modelValue: "male",
      values: ["male", "female"],
    };

    const optional = mount(Field, { props });
    const required = mount(Field, { props: { ...props, required: true } });

    expect(optional.find("#sex-input-listbox-clear").exists()).toBe(true);
    expect(required.find("#sex-input-listbox-clear").exists()).toBe(false);
  });
});
