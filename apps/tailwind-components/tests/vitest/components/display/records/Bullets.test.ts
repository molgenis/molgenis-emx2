import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Bullets from "../../../../../app/components/display/records/Bullets.vue";
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

describe("records/Bullets.vue", () => {
  it("renders one bulleted anchor per record, with title text only", () => {
    const wrapper = mount(Bullets, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const items = wrapper.findAll("li");
    expect(items.length).toBe(2);

    const anchors = wrapper.findAll("a");
    expect(anchors.length).toBe(2);
    expect(anchors[0].attributes("href")).toBe("/records/Tweety");
    expect(anchors[0].text()).toBe("Tweety");
    expect(anchors[1].attributes("href")).toBe("/records/Sylvester");
    expect(anchors[1].text()).toBe("Sylvester");
    expect(anchors[0].classes()).toContain("underline");

    expect(wrapper.text()).not.toContain("3");
  });

  it("renders a nav landmark, labelled, wrapping the ul/li list, when linkTo is passed", () => {
    const wrapper = mount(Bullets, {
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
  });

  it("takes the nav's accessible name from navLabel", () => {
    const wrapper = mount(Bullets, {
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

  it("renders no anchor and no nav when linkTo is not passed", () => {
    const wrapper = mount(Bullets, {
      props: { rows, titleTemplate: "${name}" },
      global,
    });

    expect(wrapper.find("a").exists()).toBe(false);
    expect(wrapper.find("nav").exists()).toBe(false);
    const items = wrapper.findAll("li");
    expect(items[0].text()).toBe("Tweety");
  });

  it("renders no title text when titleTemplate resolves empty", () => {
    const wrapper = mount(Bullets, {
      props: {
        rows: [{ name: "LL", year: "2006" }],
        titleTemplate: "",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
      global,
    });

    const anchor = wrapper.find("li a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
