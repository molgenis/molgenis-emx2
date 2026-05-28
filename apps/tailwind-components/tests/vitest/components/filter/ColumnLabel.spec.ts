import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import ColumnLabel from "../../../../app/components/filter/ColumnLabel.vue";

describe("ColumnLabel", () => {
  it("renders both parts and exactly one separator when labelParts has 2 elements", () => {
    const wrapper = mount(ColumnLabel, {
      props: { labelParts: ["Owner", "City"] },
    });
    expect(wrapper.text()).toContain("Owner");
    expect(wrapper.text()).toContain("City");
    const separators = wrapper.findAll('[aria-hidden="true"]');
    expect(separators).toHaveLength(1);
  });

  it("separator is aria-hidden", () => {
    const wrapper = mount(ColumnLabel, {
      props: { labelParts: ["Owner", "City"] },
    });
    const separator = wrapper.find('[aria-hidden="true"]');
    expect(separator.exists()).toBe(true);
    expect(separator.attributes("aria-hidden")).toBe("true");
  });

  it("host aria-label is the joined path when nested", () => {
    const wrapper = mount(ColumnLabel, {
      props: { labelParts: ["Owner", "City"] },
    });
    expect(wrapper.attributes("aria-label")).toBe("Owner → City");
  });

  it("renders plain text and no separator when only label is given", () => {
    const wrapper = mount(ColumnLabel, {
      props: { label: "Species" },
    });
    expect(wrapper.text()).toBe("Species");
    expect(wrapper.find('[aria-hidden="true"]').exists()).toBe(false);
  });

  it("renders plain text and no separator for single-element labelParts", () => {
    const wrapper = mount(ColumnLabel, {
      props: { label: "Species", labelParts: ["Species"] },
    });
    expect(wrapper.text()).toBe("Species");
    expect(wrapper.find('[aria-hidden="true"]').exists()).toBe(false);
  });

  it("renders N-1 separators for N-part labelParts", () => {
    const wrapper = mount(ColumnLabel, {
      props: { labelParts: ["A", "B", "C"] },
    });
    const separators = wrapper.findAll('[aria-hidden="true"]');
    expect(separators).toHaveLength(2);
    expect(wrapper.attributes("aria-label")).toBe("A → B → C");
  });
});
