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
import { enhanceMetadata } from "../../../../app/utils/enhanceMetadata";

const fetchGraphqlMock = vi.fn();
const fetchMetadataMock = vi.fn();
const fetchTableDataMock = vi.fn();

vi.mock("../../../../app/composables/fetchGraphql", () => ({
  default: (...args: unknown[]) => fetchGraphqlMock(...args),
}));
vi.mock("../../../../app/composables/fetchMetadata", () => ({
  default: (...args: unknown[]) => fetchMetadataMock(...args),
}));
vi.mock("../../../../app/composables/fetchTableData", () => ({
  default: (...args: unknown[]) => fetchTableDataMock(...args),
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
    fetchTableDataMock.mockReset();
    fetchTableDataMock.mockResolvedValue({ rows: [], count: 0 });
  });

  it("asks the derived query for the record with the path's key, and shows breadcrumbs where only the record links", async () => {
    fetchGraphqlMock.mockResolvedValue({
      Collections: [{ id: "c1", name: "Cohort one" }],
    });
    const wrapper = await mountAt("Collections/c1");

    expect(fetchGraphqlMock).toHaveBeenCalledWith(
      "demo",
      deriveRecordQuery(enhanceMetadata(schema, tableConfigs), "Collections"),
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

  it("links a row in a nested list to the parent path, the column's route and the row key", async () => {
    fetchGraphqlMock.mockResolvedValue({
      Collections: [
        {
          id: "c1",
          subpopulations: [{ resource: { id: "c1" }, name: "pop1" }],
          subpopulations_agg: { count: 1 },
        },
      ],
    });
    const wrapper = await mountAt("Collections/c1");

    expect(
      wrapper
        .find(
          "a[href='/display/record/demo/Collections/c1/subpopulations/pop1']"
        )
        .exists()
    ).toBe(true);
  });

  it("shows a displayed list into another schema, fetched from that schema and not seeded from the record", async () => {
    const withForeignList: ITableMetaData = {
      ...collections,
      columns: [
        ...collections.columns,
        {
          id: "organisationsInvolved",
          label: "Organisations",
          columnType: "REF_ARRAY",
          refTableId: "Organisations",
          refSchemaId: "other",
          refLabelDefault: "${name}",
        },
      ],
    };
    const organisations: ITableMetaData = {
      id: "Organisations",
      name: "Organisations",
      label: "Organisations",
      schemaId: "demo",
      tableType: "DATA",
      columns: [{ id: "name", label: "Name", columnType: "STRING", key: 1 }],
    };
    fetchMetadataMock.mockResolvedValue({
      ...schema,
      tables: [withForeignList, subpopulations, organisations],
    });
    fetchGraphqlMock.mockResolvedValue({
      Collections: [
        {
          id: "c1",
          organisationsInvolved: [{ name: "UMCG" }],
          organisationsInvolved_agg: { count: 1 },
        },
      ],
    });
    const wrapper = await mountAt("Collections/c1");

    expect(wrapper.find("#record-error").exists()).toBe(false);
    expect(fetchTableDataMock).toHaveBeenCalledWith(
      "other",
      "Organisations",
      expect.anything()
    );
  });
});
