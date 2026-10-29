import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import InputEnum from "../../../../app/components/input/Enum.vue";

const valuesOf = (count: number) =>
  Array.from({ length: count }, (_, i) => `V${i + 1}`);

const checkedValues = (wrapper: ReturnType<typeof mount>, type: string) =>
  wrapper
    .findAll(`input[type="${type}"]`)
    .map((input) => input.element as HTMLInputElement)
    .filter((input) => input.checked)
    .map((input) => input.value);

describe("Enum input, scalar", () => {
  it("renders a radio group in the order of values, at any length", () => {
    const values = valuesOf(25);
    const wrapper = mount(InputEnum, {
      props: { id: "e", allowedValues: values },
    });

    const radios = wrapper.findAll('input[type="radio"]');
    expect(radios.map((r) => (r.element as HTMLInputElement).value)).toEqual(
      values
    );
  });

  it("shows the stored value selected", () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", allowedValues: valuesOf(3), modelValue: "V3" },
    });

    expect(checkedValues(wrapper, "radio")).toEqual(["V3"]);
  });

  it("clears a stored radio value when not required", async () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        allowedValues: valuesOf(3),
        modelValue: "V2",
        required: false,
      },
    });

    await wrapper.find("#e-radio-group-clear").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
  });

  it("offers no clear button for a required radio group", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        allowedValues: valuesOf(3),
        modelValue: "V2",
        required: true,
      },
    });

    expect(wrapper.find("#e-radio-group-clear").exists()).toBe(false);
  });

  it("emits the picked radio value", async () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", allowedValues: valuesOf(3), modelValue: null },
    });

    await wrapper.find('input[type="radio"][value="V2"]').setValue(true);

    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual(["V2"]);
  });

  it("wires description and error to the radio group", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        allowedValues: valuesOf(3),
        describedBy: "e-description e-error",
      },
    });

    expect(
      wrapper.find("#e-radio-group").attributes("aria-describedby")
    ).toEqual("e-description e-error");
  });
});

describe("Enum input, array", () => {
  it("always renders a checkbox group in the order of values", () => {
    const values = valuesOf(25);
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        allowedValues: values,
        modelValue: null,
      },
    });

    const boxes = wrapper.findAll('input[type="checkbox"]');
    expect(boxes.map((b) => (b.element as HTMLInputElement).value)).toEqual(
      values
    );
  });

  it("shows the stored values checked", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        allowedValues: valuesOf(3),
        modelValue: ["V1", "V3"],
      },
    });

    expect(checkedValues(wrapper, "checkbox")).toEqual(["V1", "V3"]);
  });

  it("clears the stored values when not required", async () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        allowedValues: valuesOf(3),
        modelValue: ["V1"],
        required: false,
      },
    });

    await wrapper.find("#e-checkbox-group-clear").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[[]]]);
  });

  it("offers no clear button when required", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        allowedValues: valuesOf(3),
        modelValue: ["V1"],
        required: true,
      },
    });

    expect(wrapper.find("#e-checkbox-group-clear").exists()).toBe(false);
  });

  it("wires description and error to the checkbox group", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        allowedValues: valuesOf(3),
        describedBy: "e-description e-error",
      },
    });

    expect(
      wrapper.find("#e-checkbox-group").attributes("aria-describedby")
    ).toEqual("e-description e-error");
  });
});
