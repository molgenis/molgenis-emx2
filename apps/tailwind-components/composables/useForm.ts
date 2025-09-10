import {
  computed,
  ref,
  reactive,
  watch,
  type MaybeRef,
  toRef,
  toValue,
} from "vue";
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
import logger from "../utils/logger";
import {
  getColumnError,
  isColumnVisible,
} from "../../molgenis-components/src/components/forms/formUtils/formUtils";
import { useSession } from "#imports";
import { SessionExpiredError } from "../utils/sessionExpiredError";
import fetchRowPrimaryKey from "./fetchRowPrimaryKey";

export default function useForm(
  tableMetadata: MaybeRef<ITableMetaData>,
  formValueRef: MaybeRef<Record<columnId, columnValue>>,
  scrollTo: (id: string) => void = () => {}
) {
  const visibleMap = reactive<Record<columnId, boolean>>({});
  const errorMap = ref<Record<columnId, string>>({});
  const currentSection = ref<columnId>();
  const currentHeading = ref<columnId>();

  const metadata = computed(() => toValue(tableMetadata));
  const formValues = toRef(formValueRef);

  const init = () => {
    console.log("init form");
    metadata.value?.columns.forEach((column) => {
      switch (column.columnType) {
        case "SECTION":
          visibleMap[column.id] = isColumnVisible(
            column,
            formValues.value,
            metadata.value
          )
            ? true
            : false;
          break;
        case "HEADING":
          visibleMap[column.id] =
            (!column.section || visibleMap[column.section]) &&
            isColumnVisible(column, formValues.value, metadata.value)
              ? true
              : false;
          break;
        default:
          visibleMap[column.id] =
            (!column.section || visibleMap[column.section]) &&
            (!column.heading || visibleMap[column.heading]) &&
            isColumnVisible(column, formValues.value, metadata.value)
              ? true
              : false;
      }
    });
    //reset currentSection
    currentSection.value = undefined;
  };
  watch(
    () => toRef(tableMetadata).value,
    () => {
      init();
    },
    { immediate: true }
  );

  const sections = computed(() => {
    const sectionList: IFormLegendSection[] = [];
    if (!metadata.value) return sectionList;
    for (const column of metadata.value?.columns) {
      let isActive = false;
      if (
        column.id === currentSection.value ||
        column.id === currentHeading.value
      ) {
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
          errorCount: metadata.value.columns.filter(
            (subcol) =>
              (subcol.heading === column.id || subcol.section === column.id) &&
              errorMap.value[subcol.id]
          ).length,
          type: column.columnType as HeadingType,
        };
        sectionList.push(heading);
      }
    }
    if (!sectionList.some((section) => section.label)) {
      //no real sections included, then empty list so no section menu will be shown
      return [];
    }
    return sectionList;
  });

  const gotoSectionOrHeading = (id: string) => {
    metadata.value.columns.forEach((col) => {
      //apply to the right id
      if (col.id === id) {
        if (col.columnType === "HEADING") {
          currentSection.value = col.section;
          currentHeading.value = col.id;
        } else {
          currentSection.value = id;
          currentHeading.value = undefined;
        }
        scrollTo(id + "-form-field");
      }
    });
  };

  /** return required, visible fields across all sections */
  const requiredFields = computed(() => {
    return metadata.value?.columns.filter(
      (column: IColumn) => visibleMap[column.id] && column.required
    );
  });

  /** return required and empty, visible fields across all sections */
  const emptyRequiredFields = computed(() => {
    return requiredFields.value.filter(
      (column: IColumn) => !formValues.value[column.id]
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

  const validateAllColumns = () => {
    metadata.value.columns.forEach((column) => {
      validateColumn(column);
    });
  };

  const validateColumn = (column: IColumn) => {
    const error = getColumnError(column, formValues.value, metadata.value);

    if (error) {
      errorMap.value[column.id] = error;
    } else {
      errorMap.value[column.id] = metadata.value.columns
        .filter((c) => c.validation?.includes(column.id))
        .map((c) => {
          const result = getColumnError(c, formValues.value, metadata.value);
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
    const formData = toFormData(formValues.value);
    const query = `mutation insert($value:[${tableId}Input]){insert(${tableId}:$value){message}}`;
    formData.append("query", query);

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: formData,
    }).catch((error) =>
      handleFetchError(error, "Error on inserting into database")
    );
  };

  const updateInto = (schemaId: string, tableId: string) => {
    const formData = toFormData(formValues.value);
    const query = `mutation update($value:[${tableId}Input]){update(${tableId}:$value){message}}`;
    formData.append("query", query);

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: formData,
    }).catch((error) => handleFetchError(error, "Error on updating database"));
  };

  const deleteRecord = async (schemaId: string, tableId: string) => {
    const key = await getPrimaryKey(formValues.value, tableId, schemaId);
    const query = `mutation delete($pkey:[${tableId}Input]){delete(${tableId}:$pkey){message}}`;
    const variables = { pkey: [key] };

    return $fetch(`/${schemaId}/graphql`, {
      method: "POST",
      body: {
        query,
        variables,
      },
    }).catch((error) =>
      handleFetchError(error, "Error on delete form database")
    );
  };

  const updateVisibilityOnColumnChange = (column: IColumn) => {
    metadata.value.columns
      .filter((c) => c.visible?.includes(column.id))
      .forEach((c) => {
        visibleMap[c.id] =
          //columns are not shown if section/heading is invisible
          (c.columnType === "SECTION" || !c.section || visibleMap[c.section]) &&
          (c.columnType === "HEADING" || !c.heading || visibleMap[c.heading]) &&
          isColumnVisible(c, formValues.value, metadata.value)
            ? true
            : false;
        logger.debug(
          "updating visibility for " + c.id + "=" + visibleMap[c.id]
        );
      });
  };

  const onBlurColumn = (column: IColumn) => {
    validateColumn(column);
    updateVisibilityOnColumnChange(column);
  };

  const onUpdateColumn = (column: IColumn) => {
    //only update error map if error already shown so it is removed
    if (errorMap.value[column.id]) {
      validateColumn(column);
    }
    updateVisibilityOnColumnChange(column);
  };

  const onViewColumn = (column: IColumn) => {
    if (column.columnType === "SECTION") {
      currentSection.value = column.id;
      currentHeading.value = undefined;
    } else if (column.columnType === "HEADING") {
      currentSection.value = column.section;
      currentHeading.value = column.id;
    } else {
      currentSection.value = column.section;
      currentHeading.value = column.heading;
    }
  };

  const visibleColumns = computed(() => {
    return metadata.value?.columns.filter(
      (column) =>
        visibleMap[column.id] &&
        (currentSection.value === undefined ||
          column.section === currentSection.value)
    );
  });

  const invisibleColumns = computed(() => {
    return metadata.value?.columns.filter((column) => !visibleMap[column.id]);
  });

  async function handleFetchError(error: any, message: string) {
    if (error.statusCode && error.statusCode >= 400) {
      const { hasSessionTimeout } = await useSession();
      if (await hasSessionTimeout()) {
        console.log("Session has timed out, ask for re-authentication");
        throw new SessionExpiredError(
          "Session has expired, please log in again."
        );
      }
    } else {
      console.log(message, error);
    }
  }

  const rowKey = ref<columnValue>();
  //todo, find another way to produce rowkey
  //if we refactor backend to give each row an internal molgenis_id we don't need this magic anymore
  async function updateRowKey() {
    rowKey.value = await fetchRowPrimaryKey(
      formValues.value,
      metadata.value.id,
      metadata.value.schemaId as string
    );
  }
  watch(
    () => formValues.value,
    async () => {
      if (formValues.value) {
        await updateRowKey();
      }
    }
  );

  return {
    requiredFields,
    emptyRequiredFields,
    requiredMessage,
    errorMessage,
    gotoSectionOrHeading,
    gotoNextRequiredField,
    gotoPreviousRequiredField,
    gotoNextError,
    gotoPreviousError,
    insertInto,
    updateInto,
    deleteRecord,
    onUpdateColumn,
    onBlurColumn,
    onViewColumn,
    sections,
    currentSection,
    visibleColumns,
    invisibleColumns,
    errorMap,
    validateAllColumns,
    rowKey,
  };
}
