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
  it("renders a radio group in the order of values at 20 values", () => {
    const values = valuesOf(20);
    const wrapper = mount(InputEnum, { props: { id: "e", values } });

    const radios = wrapper.findAll('input[type="radio"]');
    expect(radios.map((r) => (r.element as HTMLInputElement).value)).toEqual(
      values
    );
    expect(wrapper.find('[role="combobox"]').exists()).toBe(false);
  });

  it("renders a searchable listbox at 21 values", () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", values: valuesOf(21) },
    });

    expect(wrapper.find('[role="combobox"]').exists()).toBe(true);
    expect(wrapper.find('input[type="search"]').exists()).toBe(true);
    expect(wrapper.findAll('input[type="radio"]').length).toEqual(0);
    expect(wrapper.findAll('li[role="option"]').map((li) => li.text())).toEqual(
      ["Select an option", ...valuesOf(21)]
    );
  });

  it.each([20, 21])("shows the stored value selected at %i values", (count) => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", values: valuesOf(count), modelValue: "V3" },
    });

    if (count === 20) {
      expect(checkedValues(wrapper, "radio")).toEqual(["V3"]);
    } else {
      expect(wrapper.find('[role="combobox"]').text()).toContain("V3");
      const selected = wrapper
        .findAll('li[role="option"]')
        .filter((li) => li.attributes("aria-selected") === "true");
      expect(selected.map((li) => li.text())).toEqual(["V3"]);
    }
  });

  it("clears a stored radio value when not required", async () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        values: valuesOf(3),
        modelValue: "V2",
        required: false,
      },
    });

    await wrapper.find("#e-radio-group-clear").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
  });

  it("offers no clear button for a required radio group", () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", values: valuesOf(3), modelValue: "V2", required: true },
    });

    expect(wrapper.find("#e-radio-group-clear").exists()).toBe(false);
  });

  it("clears a stored listbox value through the placeholder option", async () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", values: valuesOf(21), modelValue: "V3" },
    });

    await wrapper.findAll('li[role="option"]')[0]!.trigger("click");

    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual([null]);
  });

  it("emits the picked radio value", async () => {
    const wrapper = mount(InputEnum, {
      props: { id: "e", values: valuesOf(3), modelValue: null },
    });

    await wrapper.find('input[type="radio"][value="V2"]').setValue(true);

    expect(wrapper.emitted("update:modelValue")?.at(-1)).toEqual(["V2"]);
  });

  it.each([3, 21])(
    "wires description and error to the control at %i values",
    (count) => {
      const wrapper = mount(InputEnum, {
        props: {
          id: "e",
          values: valuesOf(count),
          describedBy: "e-description e-error",
        },
      });

      const control =
        count === 3
          ? wrapper.find("#e-radio-group")
          : wrapper.find('[role="combobox"]');
      expect(control.attributes("aria-describedby")).toEqual(
        "e-description e-error"
      );
    }
  );

  it("labels the listbox toggle by the id the form label points at", () => {
    const wrapper = mount(InputEnum, {
      props: { id: "field-input", values: valuesOf(21) },
    });

    expect(wrapper.find('[role="combobox"]').attributes("id")).toEqual(
      "field-input"
    );
  });
});

describe("Enum input, array", () => {
  it("always renders a checkbox group in the order of values", () => {
    const values = valuesOf(25);
    const wrapper = mount(InputEnum, {
      props: { id: "e", isArray: true, values, modelValue: null },
    });

    const boxes = wrapper.findAll('input[type="checkbox"]');
    expect(boxes.map((b) => (b.element as HTMLInputElement).value)).toEqual(
      values
    );
    expect(wrapper.find('[role="combobox"]').exists()).toBe(false);
  });

  it("shows the stored values checked", () => {
    const wrapper = mount(InputEnum, {
      props: {
        id: "e",
        isArray: true,
        values: valuesOf(3),
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
        values: valuesOf(3),
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
        values: valuesOf(3),
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
        values: valuesOf(3),
        describedBy: "e-description e-error",
      },
    });

    expect(
      wrapper.find("#e-checkbox-group").attributes("aria-describedby")
    ).toEqual("e-description e-error");
  });
});
