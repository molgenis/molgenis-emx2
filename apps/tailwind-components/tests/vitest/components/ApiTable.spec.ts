import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, it } from "vitest";
import ApiTable from "../../../app/components/ApiTable.vue";
import type { ComponentMeta } from "../../../app/utils/componentMetaTypes";

const fixtureButtonMeta: ComponentMeta = {
  componentName: "FixtureButton",
  filePath: "/components/FixtureButton.vue",
  props: [
    {
      name: "type",
      type: '"primary" | "secondary" | "tertiary"',
      default: '"primary"',
      required: false,
      description: "Visual style variant",
      typeDetail: {
        kind: "union",
        options: ['"primary"', '"secondary"', '"tertiary"'],
      },
    },
    {
      name: "disabled",
      type: "boolean | undefined",
      default: "false",
      required: false,
      description: "",
    },
    {
      name: "label",
      type: "string",
      default: undefined,
      required: true,
      description: "Accessible label",
    },
  ],
  events: [
    {
      name: "click",
      type: "[MouseEvent]",
      description: "Emitted on click",
    },
  ],
  slots: [
    {
      name: "default",
      type: "{}",
      description: "",
    },
    {
      name: "icon",
      type: "{}",
      description: "Optional icon slot",
    },
  ],
};

const emptyMeta: ComponentMeta = {
  componentName: "Empty",
  filePath: "/components/Empty.vue",
  props: [],
  events: [],
  slots: [],
};

describe("ApiTable", () => {
  it("renders props table with all fixture rows", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("type");
    expect(html).toContain("disabled");
    expect(html).toContain("label");
    expect(html).toContain('"primary"');
    expect(html).toContain("Visual style variant");
  });

  it("renders events table when events present", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("click");
    expect(html).toContain("Emitted on click");
  });

  it("renders slots table when slots present", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("default");
    expect(html).toContain("icon");
    expect(html).toContain("Optional icon slot");
  });

  it("shows section headings for props, events, slots", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html.toLowerCase()).toContain("props");
    expect(html.toLowerCase()).toContain("events");
    expect(html.toLowerCase()).toContain("slots");
  });

  it("renders gracefully when all sections are empty", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: emptyMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toBeTruthy();
    expect(html).not.toContain("<table");
  });

  it("marks required props visually", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const labelRow = wrapper.html();
    expect(labelRow).toContain("label");
    expect(labelRow).toContain("required");
  });

  it("renders popover trigger button for prop with union typeDetail", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("Expand type details for type");
    expect(html).toContain('"primary"');
    expect(html).toContain('"secondary"');
    expect(html).toContain('"tertiary"');
  });

  it("renders flat type string without popover trigger for props without typeDetail", async () => {
    const wrapper = await mountSuspended(ApiTable, {
      props: {
        meta: fixtureButtonMeta,
      },
    });
    const html = wrapper.html();
    expect(html).toContain("boolean | undefined");
    expect(html).not.toContain("Expand type details for disabled");
  });
});
