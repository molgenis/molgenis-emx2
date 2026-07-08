import { mountSuspended } from "@nuxt/test-utils/runtime";
import { computed } from "vue";
import { describe, expect, it } from "vitest";
import FormLegend from "../../../app/components/form/Legend.vue";
import type { LegendSection } from "../../../../../metadata-utils/src/types";

const threeLevelSections: LegendSection[] = [
  {
    id: "section-a",
    label: "Section A",
    type: "SECTION",
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: false,
    headers: [
      {
        id: "header-a1",
        label: "Header A1",
        type: "HEADING",
        errorCount: computed(() => 0),
        isVisible: computed(() => true),
        isActive: false,
      },
      {
        id: "header-a2",
        label: "Header A2",
        type: "HEADING",
        errorCount: computed(() => 0),
        isVisible: computed(() => true),
        isActive: false,
        children: [
          {
            id: "child-a2a",
            label: "Child A2a",
            type: "HEADING",
            errorCount: computed(() => 0),
            isVisible: computed(() => true),
            isActive: false,
          },
          {
            id: "child-a2b",
            label: "Child A2b",
            type: "HEADING",
            errorCount: computed(() => 0),
            isVisible: computed(() => true),
            isActive: true,
          },
        ],
      },
    ],
  },
  {
    id: "section-b",
    label: "Section B",
    type: "SECTION",
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: false,
    headers: [
      {
        id: "header-b1",
        label: "Header B1",
        type: "HEADING",
        errorCount: computed(() => 0),
        isVisible: computed(() => true),
        isActive: false,
      },
    ],
  },
];

describe("FormLegend collapsible=true", () => {
  it("auto-expands ancestors of the active item", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    const html = wrapper.html();
    expect(html).toContain("Header A1");
    expect(html).toContain("Header A2");
    expect(html).toContain("Child A2b");
  });

  it("renders 3rd-level children under an auto-expanded header", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    expect(wrapper.html()).toContain("Child A2a");
    expect(wrapper.html()).toContain("Child A2b");
  });

  it("collapses sections without active descendants by default", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    expect(wrapper.html()).not.toContain("Header B1");
  });

  it("expands a collapsed section when its row is clicked", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    expect(wrapper.html()).not.toContain("Header B1");

    const nodeButton = wrapper.find("#form-legend-header-section-b");
    expect(nodeButton.exists()).toBe(true);
    await nodeButton.trigger("click");

    expect(wrapper.html()).toContain("Header B1");
  });

  it("collapses an expanded parent when its row is clicked", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    expect(wrapper.html()).toContain("Child A2a");

    const nodeButton = wrapper.find("#form-legend-header-header-a2");
    expect(nodeButton.exists()).toBe(true);
    await nodeButton.trigger("click");

    expect(wrapper.html()).not.toContain("Child A2a");
  });

  it("sets aria-expanded=true on an expanded node", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    const btn = wrapper.find("#form-legend-header-header-a2");
    expect(btn.attributes("aria-expanded")).toBe("true");
  });

  it("sets aria-expanded=false on a collapsed node", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    const btn = wrapper.find("#form-legend-header-section-b");
    expect(btn.attributes("aria-expanded")).toBe("false");
  });

  it("clicking a node row does NOT emit goToSection", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    const nodeButton = wrapper.find("#form-legend-header-section-a");
    await nodeButton.trigger("click");
    expect(wrapper.emitted("goToSection")).toBeFalsy();
  });

  it("clicking a leaf emits goToSection and does not toggle", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, collapsible: true },
    });
    const leafLink = wrapper.find("#form-legend-header-header-a1");
    await leafLink.trigger("click");
    expect(wrapper.emitted("goToSection")).toBeTruthy();
    expect(wrapper.emitted("goToSection")![0]).toEqual(["header-a1"]);
    expect(wrapper.html()).toContain("Header A2");
  });
});

describe("FormLegend collapsible=true expandAll=true", () => {
  it("renders all nodes' children visible without any clicking", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: {
        sections: threeLevelSections,
        collapsible: true,
        expandAll: true,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("Header A1");
    expect(html).toContain("Header A2");
    expect(html).toContain("Header B1");
    expect(html).toContain("Child A2a");
    expect(html).toContain("Child A2b");
  });

  it("shows all parent nodes with aria-expanded=true and none collapsed", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: {
        sections: threeLevelSections,
        collapsible: true,
        expandAll: true,
      },
    });
    const expanded = wrapper.findAll("[aria-expanded='true']");
    const collapsed = wrapper.findAll("[aria-expanded='false']");
    expect(expanded.length).toBeGreaterThan(0);
    expect(collapsed).toHaveLength(0);
  });

  it("collapses back to active-path-only when expandAll switches to false", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: {
        sections: threeLevelSections,
        collapsible: true,
        expandAll: true,
      },
    });
    expect(wrapper.html()).toContain("Header B1");

    await wrapper.setProps({ expandAll: false });

    expect(wrapper.html()).not.toContain("Header B1");
  });

  it("expands all when expandAll switches from false to true", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: {
        sections: threeLevelSections,
        collapsible: true,
        expandAll: false,
      },
    });
    expect(wrapper.html()).not.toContain("Header B1");

    await wrapper.setProps({ expandAll: true });

    expect(wrapper.html()).toContain("Header B1");
  });
});

describe("FormLegend collapsible=false (form behavior)", () => {
  it("renders all headers and children expanded with no carets", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections },
    });
    const html = wrapper.html();
    expect(html).toContain("Header A1");
    expect(html).toContain("Header A2");
    expect(html).toContain("Header B1");
    expect(html).toContain("Child A2a");
    expect(html).toContain("Child A2b");
  });

  it("shows no caret/expand buttons", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections },
    });
    expect(wrapper.findAll("[aria-expanded]")).toHaveLength(0);
  });

  it("emits goToSection when a leaf item link is clicked", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections },
    });
    const link = wrapper.find("#form-legend-header-header-a1");
    await link.trigger("click");
    expect(wrapper.emitted("goToSection")).toBeTruthy();
    expect(wrapper.emitted("goToSection")![0]).toEqual(["header-a1"]);
  });

  it("emits goToSection when a section with children is clicked", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections },
    });
    const link = wrapper.find("#form-legend-header-section-a");
    await link.trigger("click");
    expect(wrapper.emitted("goToSection")).toBeTruthy();
    expect(wrapper.emitted("goToSection")![0]).toEqual(["section-a"]);
  });

  it("ignores expandAll=true and still renders all items with no carets", async () => {
    const wrapper = await mountSuspended(FormLegend, {
      props: { sections: threeLevelSections, expandAll: true },
    });
    const html = wrapper.html();
    expect(html).toContain("Header A1");
    expect(html).toContain("Header B1");
    expect(html).toContain("Child A2a");
    expect(wrapper.findAll("[aria-expanded]")).toHaveLength(0);
  });
});
