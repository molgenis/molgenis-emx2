<script setup lang="ts">
import { computed } from "vue";
import { useAsyncData } from "#app";
import type { ISectionField } from "../../../types/types";
import type { IColumn, IRow } from "../../../../metadata-utils/src/types";
import RecordSection from "./DetailSection.vue";
import DetailPageLayout from "../layout/DetailPageLayout.vue";
import SideNav from "../SideNav.vue";
import LoadingContent from "../LoadingContent.vue";
import fetchTableMetadata from "../../composables/fetchTableMetadata";
import fetchRowData from "../../composables/fetchRowData";
import {
  isEmptyValue,
  isTopSection,
  getTitleText,
  getSubtitleText,
  getLogoColumn,
  getRoleText,
} from "../../utils/displayUtils";

const props = withDefaults(
  defineProps<{
    schemaId?: string;
    tableId?: string;
    rowId?: IRow;
    columns?: IColumn[];
    data?: Record<string, any>;
    showEmpty?: boolean;
    showSideNav?: boolean;
    columnTransform?: (columns: IColumn[]) => IColumn[];
  }>(),
  {
    showEmpty: false,
  }
);

const isSmartMode = computed(
  () => !!(props.schemaId && props.tableId && props.rowId)
);

const {
  data: metadata,
  status: metadataStatus,
  error: metadataError,
} = useAsyncData(
  `metadata-${props.schemaId}-${props.tableId}`,
  () => fetchTableMetadata(props.schemaId!, props.tableId!),
  {
    watch: [() => props.schemaId, () => props.tableId],
    immediate: isSmartMode.value,
  }
);

const {
  data: rowData,
  status: rowStatus,
  error: rowError,
} = useAsyncData(
  `row-${props.schemaId}-${props.tableId}-${JSON.stringify(props.rowId)}`,
  () => fetchRowData(props.schemaId!, props.tableId!, props.rowId!),
  {
    watch: [() => props.schemaId, () => props.tableId, () => props.rowId],
    immediate: isSmartMode.value,
  }
);

const combinedStatus = computed(() => {
  if (!isSmartMode.value) return "success";
  if (metadataStatus.value === "pending" || rowStatus.value === "pending")
    return "pending";
  if (metadataStatus.value === "error" || rowStatus.value === "error")
    return "error";
  return "success";
});

const errorText = computed(() => {
  if (!isSmartMode.value) return undefined;
  if (metadataError.value)
    return `Failed to load metadata: ${metadataError.value.message}`;
  if (rowError.value)
    return `Failed to load row data: ${rowError.value.message}`;
  return undefined;
});

const processedColumns = computed<IColumn[]>(() => {
  let columns: IColumn[];

  if (isSmartMode.value) {
    if (!metadata.value) return [];
    columns = metadata.value.columns.filter(
      (col) =>
        col.role !== "INTERNAL" &&
        (!col.id.startsWith("mg_") ||
          col.columnType === "SECTION" ||
          col.columnType === "HEADING")
    );
  } else {
    columns = props.columns || [];
  }

  return props.columnTransform ? props.columnTransform(columns) : columns;
});

const effectiveData = computed(() => {
  if (!isSmartMode.value) return props.data || {};
  return rowData.value || {};
});

interface SectionGroup {
  heading: IColumn | null;
  isSection: boolean;
  columns: ISectionField[];
}

const sections = computed<SectionGroup[]>(() => {
  const columns = processedColumns.value;
  const result: SectionGroup[] = [];

  const sectionColumns = columns.filter((c) => c.columnType === "SECTION");
  const headingColumns = columns.filter((c) => c.columnType === "HEADING");
  const dataColumns = columns.filter(
    (c) => c.columnType !== "SECTION" && c.columnType !== "HEADING"
  );

  const orphanColumns = dataColumns.filter((c) => !c.section && !c.heading);

  if (orphanColumns.length > 0) {
    result.push({
      heading: null,
      isSection: false,
      columns: orphanColumns.map((col) => ({
        meta: col,
        value: effectiveData.value[col.id],
      })),
    });
  }

  for (const section of sectionColumns) {
    const isTop = isTopSection(section);
    const sectionDirectColumns = dataColumns.filter(
      (c) => c.section === section.id && !c.heading
    );

    if (
      sectionDirectColumns.length > 0 ||
      headingColumns.some((h) => h.section === section.id)
    ) {
      result.push({
        heading: isTop ? null : section,
        isSection: !isTop,
        columns: sectionDirectColumns.map((col) => ({
          meta: col,
          value: effectiveData.value[col.id],
        })),
      });
    }

    const sectionHeadings = headingColumns.filter(
      (h) => h.section === section.id
    );

    for (const heading of sectionHeadings) {
      const headingColumns_ = dataColumns.filter(
        (c) => c.heading === heading.id
      );

      if (headingColumns_.length > 0) {
        result.push({
          heading,
          isSection: false,
          columns: headingColumns_.map((col) => ({
            meta: col,
            value: effectiveData.value[col.id],
          })),
        });
      }
    }
  }

  const orphanHeadings = headingColumns.filter((h) => !h.section);
  for (const heading of orphanHeadings) {
    const headingColumns_ = dataColumns.filter((c) => c.heading === heading.id);

    if (headingColumns_.length > 0) {
      result.push({
        heading,
        isSection: false,
        columns: headingColumns_.map((col) => ({
          meta: col,
          value: effectiveData.value[col.id],
        })),
      });
    }
  }

  return result;
});

const visibleSections = computed(() =>
  props.showEmpty
    ? sections.value
    : sections.value.filter((s) =>
        s.columns.some((col) => !isEmptyValue(col.value))
      )
);

const navSections = computed(() =>
  visibleSections.value
    .filter((s) => s.heading && !isTopSection(s.heading))
    .map((s) => ({
      id: s.heading!.id,
      label: s.heading!.label || s.heading!.id,
    }))
);

const navTitle = computed(() => {
  const keyColumns = processedColumns.value
    .filter((c) => c.key === 1)
    .map((c) => getRoleText(effectiveData.value[c.id]))
    .filter(Boolean);
  return keyColumns.join(" - ") || undefined;
});

const logoColumn = computed(() =>
  getLogoColumn(processedColumns.value, effectiveData.value)
);
const logoUrl = computed(() => {
  if (!logoColumn.value) return undefined;
  const val = effectiveData.value[logoColumn.value.id];
  return val?.url;
});

const hasSideNav = computed(
  () => props.showSideNav !== false && navSections.value.length > 0
);

const autoTitle = computed(() =>
  getTitleText(processedColumns.value, effectiveData.value)
);
const autoSubtitle = computed(() =>
  getSubtitleText(processedColumns.value, effectiveData.value)
);

const isReady = computed(() => {
  if (!isSmartMode.value) return true;
  return processedColumns.value.length > 0 && rowData.value;
});
</script>

<template>
  <LoadingContent
    :id="`detail-view-${schemaId}-${tableId}`"
    :status="combinedStatus"
    loading-text="Loading record..."
    :error-text="errorText"
    :show-slot-on-error="false"
  >
    <div v-if="isReady" class="max-w-lg mx-auto lg:px-[30px] px-0">
      <DetailPageLayout :show-side-nav="hasSideNav">
        <template #header>
          <slot v-if="$slots.header" name="header"></slot>
          <div
            v-else-if="autoTitle || autoSubtitle"
            class="flex flex-col px-5 pt-5 pb-6 lg:pb-10 lg:px-0 text-center text-title"
          >
            <h1 class="font-display text-heading-6xl">{{ autoTitle }}</h1>
            <p v-if="autoSubtitle" class="mt-1 text-body-lg">
              {{ autoSubtitle }}
            </p>
          </div>
        </template>
        <template v-if="hasSideNav" #sidebar>
          <SideNav :sections="navSections" :title="navTitle" :image="logoUrl" />
        </template>
        <template #main>
          <div class="grid lg:gap-2.5 gap-0">
            <RecordSection
              v-for="(section, index) in visibleSections"
              :key="section.heading?.id || `orphan-${index}`"
              :heading="section.heading"
              :is-section="section.isSection"
              :columns="section.columns"
              :show-empty="showEmpty"
              :schema-id="schemaId"
              :parent-row-id="rowId"
            />
          </div>

          <slot name="footer"></slot>
        </template>
      </DetailPageLayout>
    </div>
  </LoadingContent>
</template>
