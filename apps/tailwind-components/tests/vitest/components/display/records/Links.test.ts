import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Links from "../../../../../app/components/display/records/Links.vue";
import type { IRow } from "../../../../../../metadata-utils/src/types";

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

// NuxtLink resolves to vue-router's RouterLink, which the plain test
// environment has none of, so it must be stubbed down to a real <a> to
// assert on href/text the way the app's own router would render it.
const global = {
  stubs: {
    NuxtLink: {
      props: ["to"],
      template: '<a :href="to"><slot /></a>',
    },
  },
};

describe("records/Links.vue", () => {
  it("renders record titles comma-separated on one line, each its own anchor, no trailing comma", () => {
    const wrapper = mount(Links, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const anchors = wrapper.findAll("a");
    expect(anchors.length).toBe(2);
    expect(anchors[0].attributes("href")).toBe("/records/Tweety");
    expect(anchors[0].text()).toBe("Tweety");
    expect(anchors[1].attributes("href")).toBe("/records/Sylvester");
    expect(anchors[1].text()).toBe("Sylvester");
    expect(anchors[0].classes()).toContain("underline");

    expect(wrapper.text().replace(/\s+/g, " ").trim()).toBe(
      "Tweety, Sylvester"
    );
    expect(wrapper.text()).not.toContain("3");
  });

  it("renders a nav landmark, labelled, wrapping a ul/li list, when linkTo is passed", () => {
    const wrapper = mount(Links, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const nav = wrapper.find("nav");
    expect(nav.exists()).toBe(true);
    expect(nav.attributes("aria-label")).toBeTruthy();
    expect(nav.find("ul").exists()).toBe(true);
    expect(nav.findAll("li").length).toBe(2);
    expect(nav.find("li a").exists()).toBe(true);
  });

  it("takes the nav's accessible name from navLabel", () => {
    const wrapper = mount(Links, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
        navLabel: "Related patients",
      },
      global,
    });

    expect(wrapper.find("nav").attributes("aria-label")).toBe(
      "Related patients"
    );
  });

  it("renders plain comma-separated text with no anchor and no nav when linkTo is not passed", () => {
    const wrapper = mount(Links, {
      props: { rows, titleTemplate: "${name}" },
      global,
    });

    expect(wrapper.find("a").exists()).toBe(false);
    expect(wrapper.find("nav").exists()).toBe(false);
    expect(wrapper.text().replace(/\s+/g, " ").trim()).toBe(
      "Tweety, Sylvester"
    );
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(Links, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        titleTemplate: "",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const anchor = wrapper.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
