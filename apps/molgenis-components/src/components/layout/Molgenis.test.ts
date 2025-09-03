import { describe, expect, test, vi } from "vitest";
import Molgenis from "./Molgenis.vue";

describe("Molgenis component", () => {
  describe("rewriteHref", () => {
    test("simple app names such as 'tables' are rewritten as /schemaname/tables/", () => {
      const items = [
        {
          label: "Tables",
          href: "tables",
        },
      ];
      vi.stubGlobal("location", { pathname: "/my-schema/tables#/" });
      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Tables",
          href: "/my-schema/tables/",
        },
      ]);
    });

    test("app names with parameters such as 'pages/#/mypage' are rewritten as /schemaname/pages/#/mypage", () => {
      const items = [
        {
          label: "Pages",
          href: "pages/#/mypage",
        },
      ];
      vi.stubGlobal("location", { pathname: "/my-schema/pages/#/mypage" });

      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Pages",
          href: "/my-schema/pages/#/mypage",
        },
      ]);
    });

    test("app names with 'wrong' has parameters such as 'pages#/mypage' are rewritten as /schemaname/pages/#/mypage", () => {
      const items = [
        {
          label: "Pages",
          href: "pages#/mypage",
        },
      ];

      vi.stubGlobal("location", { pathname: "/my-schema/pages/#/mypage" });

      const reWritten = Molgenis.methods?.toEmx2AppLocation(items);
      expect(reWritten).toEqual([
        {
          label: "Pages",
          href: "/my-schema/pages/#/mypage",
        },
      ]);
    });
  });
});
