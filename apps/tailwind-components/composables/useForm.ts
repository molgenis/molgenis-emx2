import { type MaybeRef, type Ref, computed, toRef, ref, reactive } from "vue";
import type {
  columnValue,
  ITableMetaData,
  IColumn,
  columnId,
  HeadingType,
  IFormLegendSection,
} from "../../metadata-utils/src/types";
import { toFormData } from "../utils/toFormData";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import logger from "~/utils/logger";
import {
  getColumnError,
  isColumnVisible,
} from "../../molgenis-components/src/components/forms/formUtils/formUtils";

/**
 * Form consist of sections (if none defined a default will be created)
 * sections can contain headings and normal columns
 *
 * @param metadata
 * @param formValues
 * @param scrollTo
 */
export default function useForm(
  metadata: MaybeRef<ITableMetaData>,
  formValues: MaybeRef<Record<columnId, columnValue>>,
  scrollTo: (id: string) => void
) {
  const activeChapterId = ref<columnId>();

  /** if user wants to select only one section */
  const visibleSection = ref<string>();
  /** keep track of all errors */
  const errorMap = ref<Record<columnId, string>>({});
  /** keep track of visibility based on visible expressions */
  const visibleMap = reactive<Record<columnId, boolean>>({});
  /** keep track what section a column belongs to */
  const columnSectionMap = ref<Record<string, string>>({});

  /** model of the sections panel */
  const sections = computed(() => {
    const sections: IFormLegendSection[] = [];

    let currentSection = null;
    let currentHeader = null;
    for (const column of toRef(metadata).value.columns) {
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

    // Add a section for the top of the page if the first column is not a heading
    if (
      (currentSection || currentHeader) &&
      !["HEADING", "SECTION"].includes(
        toRef(metadata).value.columns[0].columnType
      )
    ) {
      sections.push({
        label: "_top",
        id: "_scroll_to_top",
        isActive: "_scroll_to_top" === activeChapterId.value,
        type: "SECTION",
        errorCount: 0,
      });
    }

    return sections;
  });

  /** return required, visible fields across all sections */
  const requiredFields = computed(() => {
    return toRef(metadata).value.columns.filter(
      (column: IColumn) => visibleMap[column.id] && column.required
    );
  });

  /** return required and empty, visible fields across all sections */
  const emptyRequiredFields = computed(() => {
    return requiredFields.value.filter(
      (column: IColumn) => !toRef(formValues).value[column.id]
    );
  });

  const requiredMessage = computed(() => {
    const fieldPlural =
      emptyRequiredFields.value.length > 1 ? "fields" : "field";
    if (emptyRequiredFields.value.length === 0) {
      return "All required fields are filled";
    }
    return `${emptyRequiredFields.value.length}/${requiredFields.value.length} required ${fieldPlural} left`;
  });

  const errorMessage = computed(() => {
    const errorCount = Object.values(errorMap.value).filter(
      (value) => value !== ""
    ).length;
    const fieldLabel = errorCount === 1 ? "field requires" : "fields require";
    return errorCount > 0
      ? `${errorCount} ${fieldLabel} attention before you can save this cohort`
      : "";
  });

  const currentRequiredFieldId = ref<columnId | null>(null);

  const gotoNextRequiredField = () => {
    if (!emptyRequiredFields.value || emptyRequiredFields.value.length === 0) {
      return;
    }
    if (currentRequiredFieldId.value === null) {
      currentRequiredFieldId.value = emptyRequiredFields.value[0].id;
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredFieldId.value);
      const nextIndex = currentIndex + 1;
      currentRequiredFieldId.value =
        emptyRequiredFields.value[
          nextIndex >= emptyRequiredFields.value.length ? 0 : nextIndex
        ].id;
    }

    scrollTo(`${currentRequiredFieldId.value}-form-field`);
  };

  const validateColumn = (column: IColumn) => {
    logger.debug("validate " + column.id);

    const error = getColumnError(
      column,
      toRef(formValues).value,
      toRef(metadata).value
    );

    if (error) {
      errorMap.value[column.id] = error;
    } else {
      errorMap.value[column.id] = toRef(metadata)
        .value.columns.filter((c) => c.validation?.includes(column.id))
        .map((c) => {
          const result = getColumnError(
            c,
            toRef(formValues).value,
            toRef(metadata).value
          );
          return result;
        })
        .join("");
    }

    // remove empty entries from the map
    Object.entries(errorMap.value).forEach(([key, value]) => {
      if (value == "" || value == undefined || value == null) {
        delete errorMap.value[key];
      }
    });
  };

  const gotoPreviousRequiredField = () => {
    if (!emptyRequiredFields.value) {
      return;
    }
    if (currentRequiredFieldId.value === null) {
      currentRequiredFieldId.value = emptyRequiredFields.value[0].id;
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredFieldId.value);
      const prevIndex = currentIndex - 1;
      currentRequiredFieldId.value =
        emptyRequiredFields.value[
          prevIndex < 0 ? emptyRequiredFields.value.length - 1 : prevIndex
        ].id;
    }

    scrollTo(`${currentRequiredFieldId.value}-form-field`);
  };

  const currentErrorFieldId = ref<columnId | null>(null);

  const gotoPreviousError = () => {
    const keys = Object.keys(errorMap.value);
    const currentIndex = keys.indexOf(currentErrorFieldId.value ?? "");
    const prevIndex = currentIndex - 1;
    const previousErrorColumnId =
      keys[prevIndex < 0 ? keys.length - 1 : prevIndex];

    scrollTo(`${previousErrorColumnId}-form-field`);
  };

  const gotoNextError = () => {
    const keys = Object.keys(errorMap.value);
    const currentIndex = keys.indexOf(currentErrorFieldId.value ?? "");
    const nextIndex = currentIndex + 1;
    const nextErrorColumnId = keys[nextIndex >= keys.length ? 0 : nextIndex];

    scrollTo(`${nextErrorColumnId}-form-field`);
  };

  const insertInto = (schemaId: string, tableId: string) => {
    const formData = toFormData(toRef(formValues).value);
    const query = `mutation insert($value:[${tableId}Input]){insert(${tableId}:$value){message}}`;
    formData.append("query", query);

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: formData,
    });
  };

  const updateInto = (schemaId: string, tableId: string) => {
    const formData = toFormData(toRef(formValues).value);
    const query = `mutation update($value:[${tableId}Input]){update(${tableId}:$value){message}}`;
    formData.append("query", query);

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: formData,
    });
  };

  const deleteRecord = async (schemaId: string, tableId: string) => {
    const key = await getPrimaryKey(toRef(formValues).value, tableId, schemaId);
    const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
    const variables = { pkey: [key] };

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: {
        query,
        variables,
      },
    });
  };

  const isColumnSectionVisible = (column: IColumn) => {
    return (
      !visibleSection.value ||
      columnSectionMap.value[column.id] === visibleSection.value
    );
  };

  const onUpdateColumn = (column: IColumn, $event: columnValue) => {
    if (errorMap.value[column.id]) {
      validateColumn(column);
    }
    toRef(metadata)
      .value.columns.filter((c) => c.visible?.includes(column.id))
      .forEach((c) => {
        visibleMap[c.id] =
          isColumnSectionVisible(c) &&
          isColumnVisible(c, toRef(formValues).value, toRef(metadata).value)
            ? true
            : false;
        logger.debug(
          "updating visibility for " + c.id + "=" + visibleMap[c.id]
        );
      });
  };

  return {
    errorMap,
    visibleMap,
    requiredFields,
    emptyRequiredFields,
    requiredMessage,
    errorMessage,
    gotoNextRequiredField,
    gotoPreviousRequiredField,
    gotoNextError,
    gotoPreviousError,
    insertInto,
    updateInto,
    deleteRecord,
    onUpdateColumn,
    sections,
  };
}
