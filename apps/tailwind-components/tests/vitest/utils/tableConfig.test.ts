import { describe, expect, test } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import type { DisplayConfig } from "../../../app/types/display";
import type { TableConfig } from "../../../types/recordConfig";
import {
  listDisplay,
  resolveTableConfig,
} from "../../../app/utils/tableConfig";

const owner: ITableMetaData = {
  id: "Owner",
  label: "Owner",
  tableType: "DATA",
  schemaId: "demo",
  columns: [
    { id: "name", label: "Name", columnType: "STRING", key: 1 },
    {
      id: "pets",
      label: "Pets",
      columnType: "REFBACK",
      refTableId: "Pet",
      refBackId: "owner",
    },
  ],
};

const petDisplay: DisplayConfig = {
  layout: "CARDS",
  titleColumnId: "name",
  detailColumnIds: ["x"],
};

const configs: Record<string, TableConfig> = {
  Pet: { display: petDisplay },
  Owner: { columns: { pets: { route: "pets" } } },
};

describe("resolveTableConfig", () => {
  test("a column entry without display takes the target table's display and keeps its route", () => {
    expect(resolveTableConfig(configs, owner).columns?.["pets"]).toEqual({
      route: "pets",
      display: petDisplay,
    });
  });

  test("the entry's display wins, key by key, over the target table's", () => {
    const resolved = resolveTableConfig(
      {
        ...configs,
        Owner: {
          columns: {
            pets: { display: { layout: "LINKS", detailColumnIds: [] } },
          },
        },
      },
      owner
    );
    expect(resolved.columns?.["pets"]?.display).toEqual({
      layout: "LINKS",
      titleColumnId: "name",
      detailColumnIds: [],
    });
  });

  test("a column with no entry is not a list, even when its target has a display", () => {
    const resolved = resolveTableConfig(
      { Pet: { display: petDisplay } },
      owner
    );
    expect(listDisplay(resolved, "pets")).toBeUndefined();
  });

  test("an entry with no layout, its own or its target's, is not a list but keeps its route", () => {
    const resolved = resolveTableConfig(
      { Owner: { columns: { pets: { route: "pets" } } } },
      owner
    );
    expect(listDisplay(resolved, "pets")).toBeUndefined();
    expect(resolved.columns?.["pets"]?.route).toEqual("pets");
  });
});

describe("listDisplay", () => {
  test("returns the merged display of a list column", () => {
    expect(listDisplay(resolveTableConfig(configs, owner), "pets")).toEqual(
      petDisplay
    );
  });
});
