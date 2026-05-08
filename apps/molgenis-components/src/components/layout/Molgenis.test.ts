import { describe, expect, test, vi } from "vitest";
import { mount } from "@vue/test-utils";
import Molgenis from "./Molgenis.vue";
import { MenuItem } from "../../Interfaces/MenuItem";

vi.mock("axios", () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn().mockResolvedValue({ data: { data: { _settings: [] } } }),
    // add other axios methods if needed
  },
  get: vi.fn().mockResolvedValue({ data: [] }),
  post: vi.fn().mockResolvedValue({ data: { data: { _settings: [] } } }),
}));

describe("Molgenis component", () => {
  describe("rewriteHref", () => {
    test("simple app names such as 'tables' are rewritten as /schemaname/tables/", () => {
      const items = [
        {
          label: "Tables",
          href: "tables",
          submenu: [],
        },
      ] as MenuItem[];
      vi.stubGlobal("location", { pathname: "/my-schema/tables#/" });

      const wrapper = mount(Molgenis, {
        props: {
          title: "Test Title",
          menuItems: items,
        },
        mocks: { $route: { path: "/my-schema/tables#/" } },
      });

      expect(wrapper.html()).toContain([
        '<li class="nav-item dropdown"><a class="nav-link" href="/my-schema/tables/" target="_self">Tables</a></li>',
      ]);
    });

    test("app names with parameters such as 'pages/#/mypage' are rewritten as /schemaname/pages/#/mypage", async () => {
      const items = [
        {
          label: "Pages",
          href: "pages/#/mypage",
          submenu: [],
        },
      ];
      vi.stubGlobal("location", { pathname: "/my-schema/pages/#/mypage" });

      const wrapper = await mount(Molgenis, {
        props: {
          title: "Test Title",
          menuItems: items,
        },
      });

      expect(wrapper.html()).toContain([
        '<li class="nav-item dropdown"><a class="nav-link" href="/my-schema/pages/#/mypage" target="_self">Pages</a></li>',
      ]);
    });

    test("app names with 'wrong' has parameters such as 'pages#/mypage' are rewritten as /schemaname/pages/#/mypage", () => {
      const items = [
        {
          label: "Pages",
          href: "pages#/mypage",
          submenu: [],
        },
      ];

      vi.stubGlobal("location", { pathname: "/my-schema/pages/#/mypage" });

      const wrapper = mount(Molgenis, {
        props: {
          title: "Test Title",
          menuItems: items,
        },
      });

      expect(wrapper.html()).toContain([
        '<li class="nav-item dropdown"><a class="nav-link" href="/my-schema/pages/#/mypage" target="_self">Pages</a></li>',
      ]);
    });

    test("hrefs are not rewritten when on /apps/central/ (no context path)", () => {
      const items = [
        { label: "Databases", href: "/apps/central/", active: true },
        { label: "Help", href: "/apps/docs/" },
      ] as MenuItem[];

      vi.stubGlobal("location", { pathname: "/apps/central/" });

      const wrapper = mount(Molgenis, {
        props: { title: "Test Title", menuItems: items },
      });

      expect(wrapper.html()).toContain('href="/apps/central/"');
      expect(wrapper.html()).toContain('href="/apps/docs/"');
    });

    test("absolute hrefs are prefixed with context path when on /molgenis/apps/central/", () => {
      const items = [
        { label: "Databases", href: "/apps/central/", active: true },
        { label: "Help", href: "/apps/docs/" },
      ] as MenuItem[];

      vi.stubGlobal("location", { pathname: "/molgenis/apps/central/" });

      const wrapper = mount(Molgenis, {
        props: { title: "Test Title", menuItems: items },
      });

      expect(wrapper.html()).toContain('href="/molgenis/apps/central/"');
      expect(wrapper.html()).toContain('href="/molgenis/apps/docs/"');
    });

    test("schema menu items are prefixed with context path on schema page", () => {
      const items = [
        { label: "Tables", href: "tables", submenu: [] },
      ] as MenuItem[];

      vi.stubGlobal("location", { pathname: "/molgenis/my-schema/tables/" });

      const wrapper = mount(Molgenis, {
        props: { title: "Test Title", menuItems: items },
      });

      expect(wrapper.html()).toContain('href="/molgenis/my-schema/tables/"');
    });

    test("menu items without submenu do not crash", () => {
      const items = [
        { label: "Databases", href: "/apps/central/" },
        { label: "Help", href: "/apps/docs/" },
      ] as unknown as MenuItem[];

      vi.stubGlobal("location", { pathname: "/my-schema/tables" });

      expect(() =>
        mount(Molgenis, {
          props: { title: "Test Title", menuItems: items },
        })
      ).not.toThrow();
    });
  });
});
