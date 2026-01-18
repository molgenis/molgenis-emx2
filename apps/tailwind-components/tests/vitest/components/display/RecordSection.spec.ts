import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import RecordSection from "../../../../app/components/display/RecordSection.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";
import type { ISectionColumn } from "../../../../types/types";

describe("RecordSection", () => {
  const sectionHeading: IColumn = {
    id: "general",
    label: "General Information",
    columnType: "SECTION",
    description: "Basic details",
  };

  const heading: IColumn = {
    id: "contact",
    label: "Contact Details",
    columnType: "HEADING",
  };

  const columns: ISectionColumn[] = [
    {
      meta: { id: "name", label: "Name", columnType: "STRING" },
      value: "Test Value",
    },
    {
      meta: { id: "age", label: "Age", columnType: "INT" },
      value: 42,
    },
  ];

  const columnsWithEmpty: ISectionColumn[] = [
    {
      meta: { id: "name", label: "Name", columnType: "STRING" },
      value: "Test",
    },
    {
      meta: { id: "empty", label: "Empty", columnType: "STRING" },
      value: null,
    },
  ];

  it("renders section heading with text-heading-4xl when isSection=true", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: sectionHeading,
        isSection: true,
        columns,
      },
    });

    const h2 = wrapper.find("h2");
    expect(h2.exists()).toBe(true);
    expect(h2.text()).toBe("General Information");
    expect(h2.classes()).toContain("text-heading-4xl");
  });

  it("renders heading with text-xl when isSection=false", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading,
        isSection: false,
        columns,
      },
    });

    const h2 = wrapper.find("h2");
    expect(h2.exists()).toBe(true);
    expect(h2.text()).toBe("Contact Details");
    expect(h2.classes()).toContain("text-xl");
  });

  it("renders without heading when heading is null", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: null,
        columns,
      },
    });

    const h2 = wrapper.find("h2");
    expect(h2.exists()).toBe(false);
  });

  it("renders heading description when provided", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: sectionHeading,
        isSection: true,
        columns,
      },
    });

    expect(wrapper.text()).toContain("Basic details");
  });

  it("hides empty columns by default", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: sectionHeading,
        isSection: true,
        columns: columnsWithEmpty,
        showEmpty: false,
      },
    });

    expect(wrapper.text()).toContain("Name");
    expect(wrapper.text()).not.toContain("Empty");
  });

  it("shows empty columns when showEmpty=true", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: sectionHeading,
        isSection: true,
        columns: columnsWithEmpty,
        showEmpty: true,
      },
    });

    expect(wrapper.text()).toContain("Name");
    expect(wrapper.text()).toContain("Empty");
  });

  it("renders all column labels", () => {
    const wrapper = mount(RecordSection, {
      props: {
        heading: sectionHeading,
        isSection: true,
        columns,
      },
    });

    expect(wrapper.text()).toContain("Name");
    expect(wrapper.text()).toContain("Age");
  });
});
