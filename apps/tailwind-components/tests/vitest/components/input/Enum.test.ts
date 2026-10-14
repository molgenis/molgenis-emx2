import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import InputEnum from "../../../../app/components/input/Enum.vue";

const valuesOf = (count: number) =>
  Array.from({ length: count }, (_, i) => `V${i + 1}`);

const mountEnum = (props: Record<string, unknown>) =>
  mount(InputEnum, {
    attachTo: document.body,
    props: { id: "e", values: valuesOf(3), ...props },
  });

type Wrapper = ReturnType<typeof mountEnum>;

const toggle = (wrapper: Wrapper) => wrapper.find("button#e");
const optionTexts = (wrapper: Wrapper) =>
  wrapper.findAll('ul[role="listbox"] li[role="option"]').map((o) => o.text());
const chipTexts = (wrapper: Wrapper) =>
  wrapper.findAll('button[aria-label^="Remove "]').map((c) => c.text());

enableAutoUnmount(afterEach);

afterEach(() => {
  document.body.innerHTML = "";
});

describe("Enum input, scalar", () => {
  it("renders a searchable listbox with the values in order, at any length", () => {
    const values = valuesOf(25);
    const wrapper = mountEnum({ values });

    expect(optionTexts(wrapper)).toEqual(values);
    expect(wrapper.find('input[type="search"]').exists()).toBe(true);
    expect(wrapper.find('ul[role="listbox"]').attributes()).not.toHaveProperty(
      "aria-multiselectable"
    );
  });

  it("renders an empty listbox for an enum without values", () => {
    const wrapper = mountEnum({ values: [] });

    expect(toggle(wrapper).exists()).toBe(true);
    expect(optionTexts(wrapper)).toEqual([]);
  });

  it("shows the stored value", () => {
    const wrapper = mountEnum({ modelValue: "V3" });

    expect(toggle(wrapper).text()).toBe("V3");
  });

  it("search narrows the options by case-insensitive substring", async () => {
    const wrapper = mountEnum({
      values: ["Alpha", "Beta", "alphabet", "Gamma"],
    });
    await toggle(wrapper).trigger("click");
    await wrapper.find('input[type="search"]').setValue("ALPH");

    expect(optionTexts(wrapper)).toEqual(["Alpha", "alphabet"]);
  });

  it("emits the picked value", async () => {
    const wrapper = mountEnum({ modelValue: null });
    await toggle(wrapper).trigger("click");
    await wrapper.findAll('li[role="option"]')[1]!.trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([["V2"]]);
  });

  it("clears a stored value when not required", async () => {
    const wrapper = mountEnum({ modelValue: "V2", required: false });
    await wrapper.find("#e-listbox-clear").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[null]]);
  });

  it("offers no clear button when required", () => {
    const wrapper = mountEnum({ modelValue: "V2", required: true });

    expect(wrapper.find("#e-listbox-clear").exists()).toBe(false);
  });

  it("marks a required enum as aria-required on the toggle", () => {
    const wrapper = mountEnum({ required: true });

    expect(toggle(wrapper).attributes("aria-required")).toBe("true");
  });

  it("wires description and error to the toggle", () => {
    const wrapper = mountEnum({ describedBy: "e-description e-error" });

    expect(toggle(wrapper).attributes("aria-describedby")).toBe(
      "e-description e-error"
    );
  });

  it("forwards focus and blur", async () => {
    const wrapper = mountEnum({});
    await flushPromises();

    (toggle(wrapper).element as HTMLButtonElement).focus();
    await flushPromises();
    (toggle(wrapper).element as HTMLButtonElement).blur();
    await flushPromises();

    expect(wrapper.emitted("focus")).toHaveLength(1);
    expect(wrapper.emitted("blur")).toHaveLength(1);
  });
});

describe("Enum input, array", () => {
  it("renders a multiple listbox with the values in order", () => {
    const values = valuesOf(25);
    const wrapper = mountEnum({ isArray: true, values, modelValue: null });

    expect(optionTexts(wrapper)).toEqual(values);
    expect(wrapper.find('ul[role="listbox"]').attributes()).toHaveProperty(
      "aria-multiselectable",
      "true"
    );
  });

  it("shows the stored values as chips", () => {
    const wrapper = mountEnum({ isArray: true, modelValue: ["V3", "V1"] });

    expect(chipTexts(wrapper)).toEqual(["V3", "V1"]);
  });

  it("emits the array of picked values", async () => {
    const wrapper = mountEnum({ isArray: true, modelValue: ["V1"] });
    await toggle(wrapper).trigger("click");
    await wrapper.findAll('li[role="option"]')[2]!.trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[["V1", "V3"]]]);
  });

  it("clears the stored values when not required", async () => {
    const wrapper = mountEnum({
      isArray: true,
      modelValue: ["V1"],
      required: false,
    });
    await wrapper.find("#e-listbox-clear").trigger("click");

    expect(wrapper.emitted("update:modelValue")).toEqual([[[]]]);
  });

  it("offers no clear button when required", () => {
    const wrapper = mountEnum({
      isArray: true,
      modelValue: ["V1"],
      required: true,
    });

    expect(wrapper.find("#e-listbox-clear").exists()).toBe(false);
  });

  it("wires description and error to the toggle", () => {
    const wrapper = mountEnum({
      isArray: true,
      describedBy: "e-description e-error",
    });

    expect(toggle(wrapper).attributes("aria-describedby")).toBe(
      "e-description e-error"
    );
  });
});
