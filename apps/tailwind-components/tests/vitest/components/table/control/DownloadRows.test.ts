import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { nextTick, ref } from "vue";
import DownloadRows from "../../../../../app/components/table/control/DownloadRows.vue";
import Field from "../../../../../app/components/Field.vue";
import type { UseFilters } from "../../../../../types/filters";

function mockFilters(gqlFilter: Record<string, any>): UseFilters {
  return {
    activeFilters: ref([]),
    gqlFilter: ref(gqlFilter),
  } as unknown as UseFilters;
}

function mountDownloadRows(filters: UseFilters | null) {
  return mount(DownloadRows, {
    props: { schemaId: "pet store", tableId: "Pet", filters },
    global: { stubs: { Field: true, Button: true } },
  });
}

function findField(wrapper: ReturnType<typeof mountDownloadRows>, id: string) {
  return wrapper
    .findAllComponents(Field)
    .find((field) => field.props("id") === id);
}

function lastHref(wrapper: ReturnType<typeof mountDownloadRows>) {
  const emitted = wrapper.emitted("update:downloadHref")!;
  return emitted[emitted.length - 1]![0] as string;
}

describe("table/control/DownloadRows.vue", () => {
  it("applies a search-only filter to a csv download", () => {
    const wrapper = mountDownloadRows(mockFilters({ _search: "pooky" }));

    expect(
      findField(wrapper, "apply-filter-to-download-boolean-input")
    ).toBeDefined();
    const href = new URL(lastHref(wrapper), "http://localhost");
    expect(JSON.parse(href.searchParams.get("filter")!)).toEqual({
      _search: "pooky",
    });
  });

  it("does not apply the filter to formats that do not support it", async () => {
    const wrapper = mountDownloadRows(mockFilters({ _search: "pooky" }));

    findField(wrapper, "download-radio-input-group")!.vm.$emit(
      "update:modelValue",
      "zip"
    );
    await nextTick();

    expect(
      findField(wrapper, "apply-filter-to-download-boolean-input")
    ).toBeUndefined();
    expect(lastHref(wrapper)).toBe("/pet store/api/zip/Pet");
  });

  it("hides filter inclusion when the generated filter is empty", () => {
    const wrapper = mountDownloadRows(mockFilters({}));

    expect(
      findField(wrapper, "apply-filter-to-download-boolean-input")
    ).toBeUndefined();
    expect(lastHref(wrapper)).toBe("/pet store/api/csv/Pet");
  });
});
