import {
  computed,
  ref,
  reactive,
  watch,
  type MaybeRef,
  unref,
  isRef,
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
  formValuesRef: MaybeRef<Record<columnId, columnValue>>,
  scrollContainerId: string = ""
) {
  const metadata = ref(unref(tableMetadata));
  if (isRef(tableMetadata)) {
    watch(tableMetadata, (val) => (metadata.value = val), {
      immediate: true,
      deep: true,
    });
  }

  const formValues = ref(unref(formValuesRef));

  if (isRef(formValuesRef)) {
    watch(formValuesRef, (val) => (formValues.value = val), {
      immediate: true,
      deep: true,
    });
  }

  const visibleMap = reactive<Record<columnId, boolean | undefined>>({});
  const errorMap = ref<Record<columnId, string>>({});
  const currentSection = ref<columnId | undefined>();
  const currentHeading = ref<columnId>();
  const lastScrollTo = ref<columnId>();

  const sections = computed(() => {
    const sectionList: IFormLegendSection[] = [];
    if (!metadata.value) return sectionList;
    for (const column of metadata.value?.columns.filter(
      (c) => visibleMap[c.id]
    )) {
      let isActive = false;
      if (
        column.id === currentSection.value ||
        column.id === currentHeading.value
      ) {
        isActive = true;
      }
      if (["HEADING", "SECTION"].includes(column.columnType)) {
        const heading = {
          label: column.label,
          id: column.id,
          isActive,
          section: column.section,
          errorCount: metadata.value.columns.filter(
            (subcol) =>
              (subcol.heading === column.id ||
                (subcol.section === column.id && !subcol.heading)) &&
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
    if (!currentSection.value) {
      currentSection.value = sectionList[0]?.id;
    }
    return sectionList;
  });

  const gotoSection = (id: string) => {
    sections.value.forEach((section) => {
      //apply to the right id
      if (section.id === id) {
        if (section.type === "HEADING") {
          currentSection.value = section.section;
          currentHeading.value = section.id;
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
    return (
      requiredFields.value?.filter(
        (column: IColumn) => !formValues.value[column.id]
      ) || []
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
      currentRequiredField.value = emptyRequiredFields.value[0] ?? null;
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredField.value.id);
      const nextIndex = currentIndex + 1;
      currentRequiredField.value =
        emptyRequiredFields.value[
          nextIndex >= emptyRequiredFields.value.length ? 0 : nextIndex
        ] ?? null;
    }
    if (currentRequiredField.value) {
      currentSection.value = currentRequiredField.value.section;
      scrollTo(`${currentRequiredField.value.id}-form-field`);
    }
  };
  const gotoPreviousRequiredField = () => {
    if (!emptyRequiredFields.value) {
      return;
    }
    if (currentRequiredField.value === null) {
      if (emptyRequiredFields.value.length > 0) {
        currentRequiredField.value = emptyRequiredFields.value[0] ?? null;
      } else {
        currentRequiredField.value = null;
      }
    } else {
      const currentIndex = emptyRequiredFields.value
        .map((column) => column.id)
        .indexOf(currentRequiredField.value?.id ?? "");
      const prevIndex = currentIndex - 1;
      currentRequiredField.value =
        emptyRequiredFields.value[
          prevIndex < 0 ? emptyRequiredFields.value.length - 1 : prevIndex
        ] ?? null;
    }
    if (currentRequiredField.value) {
      scrollTo(`${currentRequiredField.value.id}-form-field`);
    }
    currentSection.value = currentRequiredField.value
      ? currentRequiredField.value.section
      : undefined;
    if (currentRequiredField.value) {
      scrollTo(`${currentRequiredField.value.id}-form-field`);
    }
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

  const currentErrorField = ref<IColumn | undefined>(undefined);

  const gotoPreviousError = () => {
    const keys = Object.keys(errorMap.value);
    if (keys.length === null) {
      return;
    }
    const currentIndex = keys.indexOf(currentErrorField.value?.id ?? "");
    const prevIndex = currentIndex - 1;
    const previousErrorColumnId =
      keys[prevIndex < 0 ? keys.length - 1 : prevIndex];

    if (previousErrorColumnId) {
      currentErrorField.value = metadata.value.columns.find(
        (col) => col.id === previousErrorColumnId
      );
      currentSection.value = currentErrorField.value?.section;
      scrollTo(`${previousErrorColumnId}-form-field`);
    }
  };

  const gotoNextError = () => {
    const keys = Object.keys(errorMap.value);
    if (keys.length === null) {
      return;
    }
    const currentIndex = keys.indexOf(currentErrorField.value?.id ?? "");
    const nextIndex = currentIndex + 1;
    const nextErrorColumnId = keys[nextIndex >= keys.length ? 0 : nextIndex];

    if (nextErrorColumnId) {
      currentErrorField.value = metadata.value.columns.find(
        (col) => col.id === nextErrorColumnId
      );
      currentSection.value = currentErrorField.value?.section;
      scrollTo(`${nextErrorColumnId}-form-field`);
    }
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

  const updateVisibility = () => {
    logger.debug("updateVisibility");
    let previousSection: IColumn | undefined = undefined;
    let sectionColumns: string[] = [];
    let previousHeading: IColumn | undefined = undefined;
    let headingColumns: string[] = [];
    metadata.value.columns.forEach((c) => {
      if (c.columnType === "SECTION") {
        if (previousSection) {
          //section is only visible if some columns are also visible
          visibleMap[previousSection.id] =
            visibleMap[previousSection.id] &&
            sectionColumns.some((columnId) => visibleMap[columnId]);
        }
        visibleMap[c.id] = isColumnVisible(c, formValues.value, metadata.value)
          ? true
          : false;
        sectionColumns = [];
        headingColumns = []; //section also resets heading
        previousSection = c;
        previousHeading = undefined;
      } else if (c.columnType === "HEADING") {
        if (previousHeading) {
          //heading is only visible if some columns are also visible
          visibleMap[previousHeading.id] =
            visibleMap[previousHeading.id] &&
            headingColumns.some((columnId) => visibleMap[columnId]);
        }
        //visible if section visible and self visible
        visibleMap[c.id] =
          (!previousSection || visibleMap[previousSection.id]) &&
          isColumnVisible(c, formValues.value, metadata.value)
            ? true
            : false;
        headingColumns = [];
        previousHeading = c;
        sectionColumns.push(c.id);
      } else {
        //visible if section is visible and heading is visible and self is visible
        visibleMap[c.id] =
          (!previousSection || visibleMap[previousSection.id]) &&
          (!previousHeading || visibleMap[previousHeading.id]) &&
          isColumnVisible(c, formValues.value, metadata.value)
            ? true
            : false;
        headingColumns.push(c.id);
        sectionColumns.push(c.id);
      }
      // empty invisible columns
      // (tricky business, users might be hurt, and we require visible expressions to point 'backwards' never 'forwards')
      if (!visibleMap[c.id]) {
        formValues.value[c.id] = undefined;
      }
    });
    //check visibility of last heading
    if (previousHeading) {
      visibleMap[(previousHeading as IColumn).id] =
        visibleMap[(previousHeading as IColumn).id] &&
        headingColumns.some((columnId) => visibleMap[columnId]);
    }
    //check visibility of last section
    if (previousSection) {
      visibleMap[(previousSection as IColumn).id] =
        visibleMap[(previousSection as IColumn).id] &&
        sectionColumns.some((columnId) => visibleMap[columnId]);
    }
  };

  const onBlurColumn = (column: IColumn) => {
    validateColumn(column);
    updateVisibility();
  };

  const onUpdateColumn = (column: IColumn) => {
    //only update error map if error already shown so it is removed
    if (errorMap.value[column.id]) {
      validateColumn(column);
    }
    updateVisibility();
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
    if (!currentSection.value) {
      currentSection.value = sections.value[0]?.id;
    }
    return metadata.value?.columns.filter(
      (column) =>
        !column.id.startsWith("mg_") &&
        visibleMap[column.id] &&
        currentSection.value === column.section
    );
  });

  const invisibleColumns = computed(() => {
    return metadata.value?.columns.filter((column) => !visibleMap[column.id]);
  });

  const nextSection = computed(() => {
    const sectionList = sections.value.filter((s) => s.type === "SECTION");
    const currentIndex = sectionList.findIndex(
      (s) => s.id === currentSection.value
    );
    if (currentIndex >= 0 && currentIndex < sectionList.length - 1) {
      return sectionList[currentIndex + 1];
    }
    return null;
  });

  const previousSection = computed(() => {
    const sectionList = sections.value.filter((s) => s.type === "SECTION");
    const currentIndex = sectionList.findIndex(
      (s) => s.id === currentSection.value
    );
    if (currentIndex > 0) {
      return sectionList[currentIndex - 1];
    }
    return null;
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
    console.log(
      "update row key, is expensive should only fire when creating an update form"
    );
    rowKey.value = await fetchRowPrimaryKey(
      formValues.value,
      metadata.value.id,
      metadata.value.schemaId as string
    );
  }

  function scrollTo(elementId: string) {
    lastScrollTo.value = elementId;
    const container = document.getElementById(scrollContainerId);

    //lazy scroll, might need to wait for elements to be mounted first
    function attemptScroll() {
      const target = document.getElementById(elementId);
      if (container && target) {
        const offset = target.offsetTop - container.offsetTop;
        container.scrollTo({ top: offset, behavior: "smooth" });
      } else {
        // try again on the next frame until the element exists
        requestAnimationFrame(attemptScroll);
      }
    }

    attemptScroll();
  }

  watch(
    () => metadata?.value,
    () => {
      //update visible expressions
      updateVisibility();
    },
    { immediate: true }
  );
  watch(
    () => formValues.value,
    async () => {
      if (formValues.value) {
        //should watch only pkey fields
        //we could make this a prop
        await updateRowKey();
      }
    }
  );

  return {
    requiredFields,
    emptyRequiredFields,
    requiredMessage,
    errorMessage,
    gotoSection,
    nextSection,
    previousSection,
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
    visibleMap,
    rowKey,
    lastScrollTo, //for debug
  };
}
