import { describe, it, expect, vi } from "vitest";
import {
  resolveEmptyRowsLabel,
  routeSearchValue,
} from "../../../../app/components/table/TableEMX2.vue";

describe("resolveEmptyRowsLabel", () => {
  it("returns 'No records found' when no filters or search are active", () => {
    expect(resolveEmptyRowsLabel(false)).toBe("No records found");
  });

  it("returns 'No data matched the filters' when filters are active", () => {
    expect(resolveEmptyRowsLabel(true)).toBe("No data matched the filters");
  });
});

describe("routeSearchValue", () => {
  it("calls setSearch when enableFilters is true", () => {
    const setSearch = vi.fn();
    const handleSearch = vi.fn();
    routeSearchValue("hello", true, setSearch, handleSearch);
    expect(setSearch).toHaveBeenCalledWith("hello");
    expect(handleSearch).not.toHaveBeenCalled();
  });

  it("calls handleSearch when enableFilters is false", () => {
    const setSearch = vi.fn();
    const handleSearch = vi.fn();
    routeSearchValue("hello", false, setSearch, handleSearch);
    expect(handleSearch).toHaveBeenCalledWith("hello");
    expect(setSearch).not.toHaveBeenCalled();
  });

  it("calls handleSearch when enableFilters is true but setSearch is null", () => {
    const handleSearch = vi.fn();
    routeSearchValue("hello", true, null, handleSearch);
    expect(handleSearch).toHaveBeenCalledWith("hello");
  });
});
