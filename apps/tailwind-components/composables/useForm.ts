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

export default function useForm(
  metadata: MaybeRef<ITableMetaData>,
  formValues: MaybeRef<Record<columnId, columnValue>>,
  scrollTo: (id: string) => void
) {
  const visibleMap = reactive<Record<columnId, boolean>>({});
  const errorMap = ref<Record<columnId, string>>({});
  const currentSection = ref<columnId>();
  const currentHeading = ref<columnId>();

  //initialize visibleMap
  toRef(metadata).value.columns.forEach((c) => {
    switch (c.columnType) {
      case "SECTION":
        visibleMap[c.id] = isColumnVisible(
          c,
          toRef(formValues).value,
          toRef(metadata).value
        )
          ? true
          : false;
        break;
      case "HEADING":
        visibleMap[c.id] =
          visibleMap[c.section] &&
          isColumnVisible(c, toRef(formValues).value, toRef(metadata).value)
            ? true
            : false;
        break;
      default:
        visibleMap[c.id] =
          visibleMap[c.section] &&
          visibleMap[c.heading] &&
          isColumnVisible(c, toRef(formValues).value, toRef(metadata).value)
            ? true
            : false;
    }
    //todo we should hide sections / headings without any visible children
    console.log(
      "updating visibility for " +
        c.id +
        "=" +
        visibleMap[c.id] +
        " " +
        JSON.stringify(c)
    );
  });

  /** model of the legend panel */
  const sections = computed(() => {
    const sectionList: IFormLegendSection[] = [];
    for (const column of toRef(metadata).value.columns) {
      let isActive = false;
      if (
        column.id === currentSection.value ||
        column.id === currentHeading.value
      ) {
        //hasActiveBeenSet = true;
        isActive = true;
      }
      if (
        ["HEADING", "SECTION"].includes(column.columnType) &&
        visibleMap[column.id]
      ) {
        const heading = {
          label: column.label,
          id: column.id,
          isActive,
          errorCount: toRef(metadata).value.columns.filter(
            (subcol) =>
              (subcol.heading === column.id || subcol.section === column.id) &&
              errorMap.value[subcol.id]
          ).length,
          type: column.columnType as HeadingType,
        };
        sectionList.push(heading);
      }
    }
    return sectionList;
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

  const currentRequiredField = ref<IColumn | null>(null);
  const gotoNextRequiredField = () => {
    if (!emptyRequiredFields.value || emptyRequiredFields.value.length === 0) {
      return;
    }
    if (currentRequiredField.value === null) {
      currentRequiredField.value = emptyRequiredFields.value[0];
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredField.value.id);
      const nextIndex = currentIndex + 1;
      currentRequiredField.value =
        emptyRequiredFields.value[
          nextIndex >= emptyRequiredFields.value.length ? 0 : nextIndex
        ];
    }
    currentSection.value = currentRequiredField.value.section;
    scrollTo(`${currentRequiredField.value.id}-form-field`);
  };
  const gotoPreviousRequiredField = () => {
    if (!emptyRequiredFields.value) {
      return;
    }
    if (currentRequiredField.value === null) {
      currentRequiredField.value = emptyRequiredFields.value[0];
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredField.value.id);
      const prevIndex = currentIndex - 1;
      currentRequiredField.value =
        emptyRequiredFields.value[
          prevIndex < 0 ? emptyRequiredFields.value.length - 1 : prevIndex
        ];
    }
    currentSection.value = currentRequiredField.value.section;
    scrollTo(`${currentRequiredField.value.id}-form-field`);
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

  const currentErrorField = ref<IColumn | null>(null);

  const gotoPreviousError = () => {
    const keys = Object.keys(errorMap.value);
    if (keys.length === null) {
      return;
    }
    const currentIndex = keys.indexOf(currentErrorField.value?.id ?? "");
    const prevIndex = currentIndex - 1;
    const previousErrorColumnId =
      keys[prevIndex < 0 ? keys.length - 1 : prevIndex];

    currentSection.value = currentErrorField.value?.section;
    scrollTo(`${previousErrorColumnId}-form-field`);
  };

  const gotoNextError = () => {
    const keys = Object.keys(errorMap.value);
    if (keys.length === null) {
      return;
    }
    const currentIndex = keys.indexOf(currentErrorField.value?.id ?? "");
    const nextIndex = currentIndex + 1;
    const nextErrorColumnId = keys[nextIndex >= keys.length ? 0 : nextIndex];

    currentSection.value = currentErrorField.value?.section;
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

  const onUpdateColumn = (column: IColumn, $event: columnValue) => {
    //update error map for this column
    if (errorMap.value[column.id]) {
      validateColumn(column);
    }
    //update visibility map for any expression that includes this column id
    toRef(metadata)
      .value.columns.filter((c) => c.visible?.includes(column.id))
      .forEach((c) => {
        visibleMap[c.id] =
          //columns are not shown if section/heading is invisible
          (c.columnType === "SECTION" || visibleMap[c.section]) &&
          (c.columnType === "HEADING" || visibleMap[c.heading]) &&
          isColumnVisible(c, toRef(formValues).value, toRef(metadata).value)
            ? true
            : false;
        logger.debug(
          "updating visibility for " + c.id + "=" + visibleMap[c.id]
        );
      });
  };

  const visibleColumns = computed(() => {
    return toRef(metadata).value.columns.filter(
      (column) => visibleMap[column.id]
    );
  });

  const invisibleColumns = computed(() => {
    return toRef(metadata).value.columns.filter(
      (column) => !visibleMap[column.id]
    );
  });

  return {
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
    visibleColumns,
    invisibleColumns,
  };
}
