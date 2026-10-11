import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Field from "../../../app/components/Field.vue";

describe("Field.vue with an enum column", () => {
  it("passes values on, so a scalar renders one radio per value", () => {
    const wrapper = mount(Field, {
      props: {
        id: "sex",
        type: "ENUM",
        modelValue: null,
        values: ["male", "female", "unknown"],
        label: "Sex",
      },
    });

    expect(
      wrapper
        .findAll('input[type="radio"]')
        .map((radio) => (radio.element as HTMLInputElement).value)
    ).toEqual(["male", "female", "unknown"]);
  });

  it("offers Clear unless the column is required", () => {
    const props = {
      id: "sex",
      type: "ENUM" as const,
      modelValue: "male",
      values: ["male", "female"],
    };

    const optional = mount(Field, { props });
    const required = mount(Field, { props: { ...props, required: true } });

    expect(optional.find("#sex-input-radio-group-clear").exists()).toBe(true);
    expect(required.find("#sex-input-radio-group-clear").exists()).toBe(false);
  });

  it("names the description and the error ids on the control", () => {
    const wrapper = mount(Field, {
      props: {
        id: "sex",
        type: "ENUM",
        modelValue: null,
        values: ["male", "female"],
        label: "Sex",
        description: "Biological sex",
        errorMessage: "Pick one",
      },
    });

    const describedBy = wrapper
      .find("#sex-input-radio-group")
      .attributes("aria-describedby")!
      .split(" ");
    expect(describedBy).toEqual(["sex-input-description", "sex-input-error"]);
    expect(wrapper.find("#sex-input-description").text()).toContain(
      "Biological sex"
    );
  });

  it("points the label at the listbox toggle above 20 values", () => {
    const wrapper = mount(Field, {
      props: {
        id: "code",
        type: "ENUM",
        modelValue: null,
        values: Array.from({ length: 21 }, (_, i) => `v${i}`),
        label: "Code",
      },
    });

    const labelFor = wrapper.find("label").attributes("for");
    expect(wrapper.find(`[role="combobox"]#${labelFor}`).exists()).toBe(true);
  });
});
