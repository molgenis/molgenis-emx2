import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import Legend from "../../../../app/components/form/Legend.vue";

describe("Legend", () => {
  test("links to the href an entry carries, and stays quiet on click", async () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          {
            id: "about",
            label: "About",
            href: "#about",
            isVisible: true,
            headers: [
              { id: "size", label: "Size", href: "#size", isVisible: true },
            ],
          },
        ],
      },
    });

    const links = wrapper.findAll("a");
    expect(links.map((link) => link.attributes("href"))).toEqual([
      "#about",
      "#size",
    ]);

    await links[0]!.trigger("click");
    expect(wrapper.emitted("goToSection")).toBeUndefined();
  });

  test("asks the form to scroll when an entry carries no href", async () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [{ id: "about", label: "About", isVisible: true }],
      },
    });

    const link = wrapper.get("a");
    expect(link.attributes("href")).toBe("#");

    await link.trigger("click");
    expect(wrapper.emitted("goToSection")).toEqual([["about"]]);
  });

  test("marks no entry current when entries are links and none is active", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          { id: "about", label: "About", href: "#about" },
          { id: "care", label: "Care", href: "#care" },
        ],
      },
    });

    expect(
      wrapper.findAll("a").map((link) => link.attributes("aria-current"))
    ).toEqual(["false", "false"]);
  });

  test("still falls back to the first entry when a form legend has no active section", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          { id: "about", label: "About" },
          { id: "care", label: "Care" },
        ],
      },
    });

    expect(
      wrapper.findAll("a").map((link) => link.attributes("aria-current"))
    ).toEqual(["true", "false"]);
  });

  test("describes an entry by its error counter only when the counter renders", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          { id: "about", label: "About", errorCount: 2 },
          { id: "care", label: "Care", errorCount: 0 },
        ],
      },
    });

    const [withErrors, withoutErrors] = wrapper.findAll("a");
    expect(withErrors!.attributes("aria-describedby")).toBe(
      `${withErrors!.attributes("id")}-error-count`
    );
    expect(withoutErrors!.attributes("aria-describedby")).toBeUndefined();
  });

  test("renders a sub-entry that says nothing about its visibility", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          {
            id: "about",
            label: "About",
            href: "#about",
            headers: [{ id: "size", label: "Size", href: "#size" }],
          },
        ],
      },
    });

    expect(wrapper.findAll("a").map((link) => link.text())).toEqual([
      "About",
      "Size",
    ]);
  });

  test("hides a sub-entry that says it is invisible", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          {
            id: "about",
            label: "About",
            href: "#about",
            headers: [
              { id: "size", label: "Size", href: "#size", isVisible: false },
            ],
          },
        ],
      },
    });

    expect(wrapper.findAll("a").map((link) => link.text())).toEqual(["About"]);
  });

  test("gives each legend on the page its own entry ids", () => {
    const page = mount(
      {
        components: { Legend },
        template: `<div>
          <Legend :sections="sections" />
          <Legend :sections="sections" />
        </div>`,
        data: () => ({ sections: [{ id: "about", label: "About" }] }),
      },
      { global: { components: { Legend } } }
    );

    const [first, second] = page.findAll("a").map((a) => a.attributes("id"));
    expect(first).toContain("about");
    expect(second).toContain("about");
    expect(first).not.toBe(second);
  });

  test("renders a title above the entry list, and nothing at all without one", () => {
    const sections = [{ id: "about", label: "About", href: "#about" }];

    const titled = mount(Legend, {
      props: { sections },
      slots: { title: "<h2>spike - dog</h2>" },
    });
    const nav = titled.get("nav").element;
    expect(nav.firstElementChild?.tagName.toLowerCase()).toBe("h2");
    expect(titled.get("nav > h2").text()).toBe("spike - dog");

    const untitled = mount(Legend, { props: { sections } });
    expect(
      untitled.get("nav").element.firstElementChild?.tagName.toLowerCase()
    ).toBe("ul");
    expect(untitled.get("nav").element.children).toHaveLength(1);
  });

  test("names its navigation landmark and labels every entry", () => {
    const wrapper = mount(Legend, {
      props: {
        sections: [
          {
            id: "about",
            label: "About",
            href: "#about",
            isVisible: true,
            headers: [
              { id: "size", label: "Size", href: "#size", isVisible: true },
            ],
          },
        ],
      },
    });

    expect(wrapper.get("nav").attributes("aria-label")).toBe(
      "Section navigation"
    );
    expect(wrapper.findAll("a").map((link) => link.text())).toEqual([
      "About",
      "Size",
    ]);
  });
});
