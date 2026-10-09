import { describe, expect, it } from "vitest";
import { ref } from "vue";
import { useDownloadColumns } from "../../../app/composables/useDownloadColumns";
import type { IColumn } from "../../../../metadata-utils/src/types";

function col(id: string, extra: Partial<IColumn> = {}): IColumn {
  return { id, label: id, columnType: "STRING", ...extra } as IColumn;
}

const pet = col("petName");
const owner = col("owner", { columnType: "REF", refTableId: "Owner" });
const weight = col("weight", { columnType: "DECIMAL" });

describe("useDownloadColumns", () => {
  it("should not be active when all columns are shown in default order", () => {
    const all = ref([pet, owner, weight]);
    const { isColumnSelectionActive } = useDownloadColumns(all, all);
    expect(isColumnSelectionActive.value).toBe(false);
  });

  it("should be active when a column is hidden", () => {
    const { isColumnSelectionActive, columnIds } = useDownloadColumns(
      ref([pet, owner, weight]),
      ref([pet, weight])
    );
    expect(isColumnSelectionActive.value).toBe(true);
    expect(columnIds.value).toEqual(["petName", "weight"]);
  });

  it("should be active when columns are reordered", () => {
    const { isColumnSelectionActive } = useDownloadColumns(
      ref([pet, owner, weight]),
      ref([weight, pet, owner])
    );
    expect(isColumnSelectionActive.value).toBe(true);
  });

  it("should not be active when no columns are shown", () => {
    const { isColumnSelectionActive } = useDownloadColumns(
      ref([pet, owner]),
      ref([])
    );
    expect(isColumnSelectionActive.value).toBe(false);
  });

  it("should append the ids as one comma separated columns param, leaving refs for the backend to expand", () => {
    const { appendColumnsParam } = useDownloadColumns(
      ref([pet, owner, weight]),
      ref([owner, pet])
    );
    const params = new URLSearchParams();
    appendColumnsParam(params);
    expect(params.get("columns")).toBe("owner,petName");
  });
});
