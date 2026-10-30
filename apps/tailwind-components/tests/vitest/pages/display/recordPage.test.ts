import { mountSuspended } from "@nuxt/test-utils/runtime";
import { flushPromises } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearNuxtData } from "#app";
import type {
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../../metadata-utils/src/types";
import RecordPage from "../../../../app/pages/display/record/[schemaId]/[...path].vue";
import { tableConfigs } from "../../../../app/demoTableConfigs";
import { deriveRecordQuery } from "../../../../app/utils/deriveRecordQuery";
import { resolveTableConfig } from "../../../../app/utils/tableConfig";

const fetchGraphqlMock = vi.fn();
const fetchMetadataMock = vi.fn();

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: (...args: unknown[]) => fetchGraphqlMock(...args),
}));
vi.mock("../../../../app/composables/fetchMetadata", () => ({
  default: (...args: unknown[]) => fetchMetadataMock(...args),
}));
vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: () => Promise.resolve({ rows: [], count: 0 }),
}));
vi.mock("../../../../app/composables/fetchTableMetadata", () => ({
  default: () => Promise.resolve({ id: "Subpopulations", columns: [] }),
}));

const collections: ITableMetaData = {
  id: "Collections",
  name: "Collections",
  label: "Collections",
  schemaId: "demo",
  tableType: "DATA",
  columns: [
    { id: "id", label: "id", columnType: "STRING", key: 1 },
    { id: "name", label: "Name", columnType: "STRING" },
    {
      id: "subpopulations",
      label: "Subpopulations",
      columnType: "REFBACK",
      refTableId: "Subpopulations",
      refSchemaId: "demo",
      refBackId: "resource",
    },
  ],
};

const subpopulations: ITableMetaData = {
  id: "Subpopulations",
  name: "Subpopulations",
  label: "Subpopulations",
  schemaId: "demo",
  tableType: "DATA",
  columns: [
    {
      id: "resource",
      label: "Resource",
      columnType: "REF",
      refTableId: "Collections",
      refSchemaId: "demo",
      key: 1,
    },
    { id: "name", label: "Name", columnType: "STRING", key: 1 },
  ],
};

const schema: ISchemaMetaData = {
  id: "demo",
  label: "demo",
  tables: [collections, subpopulations],
};

async function mountAt(path: string) {
  const wrapper = await mountSuspended(RecordPage, {
    route: `/display/record/demo/${path}`,
  });
  await flushPromises();
  return wrapper;
}

function crumbs(wrapper: Awaited<ReturnType<typeof mountAt>>) {
  const desktopList = wrapper.findAll("nav[aria-label=breadcrumb] ol")[1];
  return (desktopList?.findAll("li") ?? []).map((item) => ({
    label: item.text(),
    href: item.find("a").exists() ? item.find("a").attributes("href") : null,
  }));
}

describe("record page", () => {
  beforeEach(() => {
    clearNuxtData();
    fetchGraphqlMock.mockReset();
    fetchMetadataMock.mockReset();
    fetchMetadataMock.mockResolvedValue(schema);
  });

  it("asks the derived query for the record with the path's key, and shows breadcrumbs where only the record links", async () => {
    fetchGraphqlMock.mockResolvedValue({
      Collections: [{ id: "c1", name: "Cohort one" }],
    });
    const wrapper = await mountAt("Collections/c1");

    expect(fetchGraphqlMock).toHaveBeenCalledWith(
      "demo",
      deriveRecordQuery(
        schema,
        "Collections",
        resolveTableConfig(tableConfigs, collections)
      ),
      { filter: { id: { equals: "c1" } } }
    );
    expect(wrapper.text()).toContain("Cohort one");
    expect(crumbs(wrapper)).toEqual([
      { label: "Collections", href: null },
      { label: "c1", href: "/display/record/demo/Collections/c1" },
    ]);
  });

  it("shows a Collections record's website and a contact button linking to its contact email, above the sections", async () => {
    fetchGraphqlMock.mockResolvedValue({
      Collections: [
        {
          id: "c1",
          name: "Cohort one",
          website: "https://example.org",
          contactEmail: "contact@example.org",
        },
      ],
    });
    const wrapper = await mountAt("Collections/c1");

    const intro = wrapper.get("[data-testid=record-intro]");
    expect(intro.get("a[href='https://example.org']").text()).toBe(
      "https://example.org"
    );
    expect(intro.get("a[href='mailto:contact@example.org']").text()).toContain(
      "Contact"
    );
    expect(wrapper.get("main").element.firstElementChild).toBe(
      intro.element.parentElement
    );
  });

  it("opens a nested record by the parent key and its own, with one crumb per path segment", async () => {
    fetchGraphqlMock.mockResolvedValue({
      Subpopulations: [{ resource: { id: "c1" }, name: "pop1" }],
    });
    const wrapper = await mountAt("Collections/c1/subpopulations/pop1");

    expect(fetchGraphqlMock).toHaveBeenCalledWith(
      "demo",
      expect.stringContaining("Subpopulations(filter: $filter"),
      {
        filter: {
          resource: { id: { equals: "c1" } },
          name: { equals: "pop1" },
        },
      }
    );
    expect(crumbs(wrapper)).toEqual([
      { label: "Collections", href: null },
      { label: "c1", href: "/display/record/demo/Collections/c1" },
      { label: "subpopulations", href: null },
      {
        label: "pop1",
        href: "/display/record/demo/Collections/c1/subpopulations/pop1",
      },
    ]);
  });
});
