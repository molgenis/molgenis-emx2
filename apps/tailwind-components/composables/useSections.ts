import { type MaybeRef, type Ref, toRef, computed } from "vue";
import type { ITableMetaData } from "../../metadata-utils/src";
import type {
  columnId,
  HeadingType,
  IFormLegendSection,
} from "../../metadata-utils/src/types";

export default function useSections(
  metadata: MaybeRef<ITableMetaData>,
  activeChapterId: Ref<columnId>,
  errorMap: Ref<Record<columnId, string>>
): Ref<IFormLegendSection[]> {
  const refMeta = toRef(metadata);
  return computed(() => {
    const sections: IFormLegendSection[] = [];
    if (refMeta.value.columns.length === 0) {
      return sections;
    }

    const hasSections = refMeta.value.columns.some(
      (column) => column.columnType === "SECTION"
    );

    const hasHeadings = refMeta.value.columns.some(
      (column) => column.columnType === "HEADING"
    );

    // Add a section for the top of the page if the first column is not a heading
    if (
      (hasHeadings || hasSections) &&
      !["HEADING", "SECTION"].includes(refMeta.value.columns[0].columnType)
    ) {
      sections.push({
        label: "_top",
        id: "_scroll_to_top",
        isActive: "_scroll_to_top" === activeChapterId.value,
        type: "SECTION",
        errorCount: 0,
      });
    }

    let hasActiveBeenSet = false;
    let currentSection = null;
    let currentHeader = null;

    for (const column of refMeta.value.columns) {
      let isActive = false;
      if (column.id === activeChapterId.value) {
        hasActiveBeenSet = true;
        isActive = true;
      }
      if (["HEADING", "SECTION"].includes(column.columnType)) {
        const heading = {
          label: column.label,
          id: column.id,
          isActive,
          errorCount: 0,
          type: (column.columnType === "HEADING"
            ? "HEADING"
            : "SECTION") as HeadingType,
        };
        sections.push(heading);
        if (column.columnType === "SECTION") {
          currentSection = heading;
        }
        currentHeader = heading;
      } else {
        const errorCount = errorMap.value[column.id] ? 1 : 0;
        if (currentHeader) {
          currentHeader.errorCount += errorCount;
        }
        if (currentSection) {
          currentSection.errorCount += errorCount;
        }
      }
    }

    if (sections.length && !hasActiveBeenSet) {
      sections[0].isActive = true;
    }

    return sections;
  });
}
