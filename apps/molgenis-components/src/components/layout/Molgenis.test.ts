import { describe, expect, test, vi } from "vitest";
import { mount } from "@vue/test-utils";
import Molgenis from "./Molgenis.vue";
import { MenuItem } from "../../Interfaces/MenuItem";

describe("Molgenis component", () => {
  describe("rewriteHref", () => {
    test("simple app names such as 'tables' are rewritten as /schemaname/tables/", async () => {
      const items = [
        {
          label: "Tables",
          href: "tables",
          submenu: [],
        },
      ] as MenuItem[];
      vi.stubGlobal("location", { pathname: "/my-schema/tables#/" });

      const wrapper = await mount(Molgenis, {
        props: {
          title: "Test Title",
          menuItems: items,
        },
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

    test("app names with 'wrong' has parameters such as 'pages#/mypage' are rewritten as /schemaname/pages/#/mypage", async () => {
      const items = [
        {
          label: "Pages",
          href: "pages#/mypage",
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
  });
});
