import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Links from "../../../../../app/components/display/records/Links.vue";
import type { IRow } from "../../../../../../metadata-utils/src/types";

const rows: IRow[] = [
  { name: "Tweety", age: 3 },
  { name: "Sylvester", age: 5 },
];

describe("records/Links.vue", () => {
  it("renders record titles comma-separated on one line, each its own anchor, no trailing comma", () => {
    const wrapper = mount(Links, {
      props: {
        rows,
        titleTemplate: "${name}",
        linkTo: (row: IRow) => `/records/${row.name}`,
      },
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

  it("renders plain comma-separated text with no anchor when linkTo is not passed", () => {
    const wrapper = mount(Links, {
      props: { rows, titleTemplate: "${name}" },
    });

    expect(wrapper.find("a").exists()).toBe(false);
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
    });

    const anchor = wrapper.find("a");
    expect(anchor.exists()).toBe(true);
    expect(anchor.text()).toBe("");
    expect(wrapper.text()).not.toContain("LL");
    expect(wrapper.text()).not.toContain("2006");
  });
});
