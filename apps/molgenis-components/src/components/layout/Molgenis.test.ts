import { describe, expect, test, vi } from "vitest";
import Molgenis from "./Molgenis.vue";
import { MenuItem } from "../../Interfaces/MenuItem";

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
      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Tables",
          href: "/my-schema/tables/",
          submenu: [],
        },
      ]);
    });

    test("app names with parameters such as 'pages/#/mypage' are rewritten as /schemaname/pages/#/mypage", () => {
      const items = [
        {
          label: "Pages",
          href: "pages/#/mypage",
          submenu: [],
        },
      ];
      vi.stubGlobal("location", { pathname: "/my-schema/pages/#/mypage" });

      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Pages",
          href: "/my-schema/pages/#/mypage",
          submenu: [],
        },
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

      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Pages",
          href: "/my-schema/pages/#/mypage",
          submenu: [],
        },
      ]);
    });

    test("should make the petstore ", () => {
      const petstoreItems = [
        {
          label: "Tables",
          href: "tables",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Schema",
          href: "schema",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Up/Download",
          href: "updownload",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Reports",
          href: "reports",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Jobs & Scripts",
          href: "tasks",
          role: "Manager",
          submenu: [],
        },
        {
          label: "Graphql",
          href: "graphql-playground",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Settings",
          href: "settings",
          role: "Manager",
          submenu: [],
        },
        {
          label: "Help",
          href: "docs",
          role: "Viewer",
          submenu: [],
        },
      ] as MenuItem[];

      vi.stubGlobal("location", { pathname: "/pet store/tables#/" });

      const reWritten = Molgenis.methods?.toEmx2AppLocation(petstoreItems);
      expect(reWritten).toEqual([
        {
          label: "Tables",
          href: "/pet store/tables/",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Schema",
          href: "/pet store/schema/",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Up/Download",
          href: "/pet store/updownload/",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Reports",
          href: "/pet store/reports/",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Jobs & Scripts",
          href: "/pet store/tasks/",
          role: "Manager",
          submenu: [],
        },
        {
          label: "Graphql",
          href: "/pet store/graphql-playground/",
          role: "Viewer",
          submenu: [],
        },
        {
          label: "Settings",
          href: "/pet store/settings/",
          role: "Manager",
          submenu: [],
        },
        {
          label: "Help",
          href: "/pet store/docs/",
          role: "Viewer",
          submenu: [],
        },
      ]);
    });
  });
});
