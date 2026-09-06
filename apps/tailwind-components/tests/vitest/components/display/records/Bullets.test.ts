import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Bullets from "../../../../../app/components/display/records/Bullets.vue";
import type { IRow } from "../../../../../../metadata-utils/src/types";

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

describe("records/Bullets.vue", () => {
  it("renders one bulleted anchor per record, with title text only", () => {
    const wrapper = mount(Bullets, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
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

  it("renders no anchor when linkTo is not passed", () => {
    const wrapper = mount(Bullets, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("a").exists()).toBe(false);
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
    });

    const anchor = wrapper.find("li a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
