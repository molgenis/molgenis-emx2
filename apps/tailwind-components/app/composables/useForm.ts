import { useSession } from "./useSession";
import {
  computed,
  isRef,
  ref,
  unref,
  watch,
  type ComputedRef,
  type MaybeRef,
  type Ref,
} from "vue";
import { toFormData } from "../../../metadata-utils/src/toFormData";
import type {
  columnId,
  columnValue,
  IColumn,
  ITableMetaData,
  LegendSection,
} from "../../../metadata-utils/src/types";
import {
  getColumnError,
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import { SessionExpiredError } from "../utils/sessionExpiredError";
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

  // initialize form values with all columns to prevent reference errors in expressions
  metadata.value.columns.forEach((column: IColumn) => {
    if (
      !formValues.value.hasOwnProperty(column.id) &&
      column.columnType !== "SECTION" &&
      column.columnType !== "HEADING"
    ) {
      formValues.value[column.id] = null;
    }
  });

  const formValueKeys = metadata.value.columns.map((col) => col.id);

  const extractParamsFromExpression = (expression: string) => {
    const params: string[] = [];
    if (expression === "") {
      return params;
    }
    for (const key of formValueKeys) {
      // regex finds formValue keys used inside the expression
      const regex = new RegExp(`(?<!['"])\\b${key}\\b(?!['"])`, "g");
      if (regex.test(expression)) {
        params.push(key);
      }
    }
    return params;
  };

  // setup visibility signals
  const visibilityMap = metadata.value.columns.reduce((acc, column) => {
    const cleanExpression = column.visible?.replaceAll('"', "'") || "true";

    const exprString = column.visible;
    const params: string[] = extractParamsFromExpression(exprString || "");

    const paramsString = params.join(", ");
    const visibilityFunction = new Function(
      paramsString,
      "return " + cleanExpression
    );

    // use function with apply to pass parameters dynamically while keeping reactivity
    acc[column.id] = computed(
      () =>
        !!visibilityFunction.apply(
          null,
          params.map((p) => formValues.value[p])
        )
    );

    return acc;
  }, {} as Record<columnId, ComputedRef<boolean>>);

  const requiredMap = metadata.value.columns.reduce((acc, column) => {
    if (typeof column.required === "boolean") {
      acc[column.id] = computed(() => !!column.required);
    } else if (
      column.required?.toLocaleLowerCase() === "true" ||
      column.required?.toLocaleLowerCase() === "false"
    ) {
      const requiredBool = column.required.toLocaleLowerCase() === "true";
      acc[column.id] = computed(() => requiredBool);
    } else if (typeof column.required === "string") {
      try {
        const requiredExpression = column.required;
        const cleanExpression =
          requiredExpression.replaceAll('"', "'") || "false";
        const params: string[] =
          extractParamsFromExpression(requiredExpression);
        const paramsString = params.join(", ");
        const requiredFunction = new Function(
          paramsString,
          "return eval(`" + cleanExpression + "`)"
        );
        acc[column.id] = computed(
          () =>
            !!requiredFunction.apply(
              null,
              params.map((p) => formValues.value[p])
            )
        );
      } catch (e) {
        console.error(
          "Error creating required function for column",
          column.id,
          e
        );
        acc[column.id] = computed(() => false);
      }
    } else {
      // default to not required
      acc[column.id] = computed(() => false);
    }
    return acc;
  }, {} as Record<columnId, ComputedRef<boolean>>);

  const errorMap = ref<Record<columnId, string>>({});
  const lastScrollTo = ref<columnId>();
  const currentErrorField = ref<IColumn | undefined>(undefined);

  const sections: Ref<LegendSection[]> = ref([]);

  // first pass to get sections
  metadata.value.columns.forEach((column) => {
    if (column.columnType === "SECTION") {
      const columns = metadata.value.columns.filter(
        (col) =>
          col.section === column.id &&
          !col.id.startsWith("mg_") &&
          col.id !== column.id
      );

      const section: LegendSection = {
        id: column.id,
        label: column.label,
        type: "SECTION",
        headers: [],
        isVisible: computed(() =>
          columns.some((col) => visibilityMap[col.id]?.value === true)
        ),
        isActive: computed(
          () =>
            visibleColumnIds.value.has(column.id) ||
            section.headers.some((header) =>
              visibleColumnIds.value.has(header.id)
            )
        ),
        errorCount: computed(() => {
          return columns.reduce((acc, col) => {
            if (visibleColumnErrors.value[col.id]) {
              return acc + 1;
            }
            return acc;
          }, 0);
        }),
      };

      sections.value.push(section);
    }
  });

  // second pass to get headings and colums
  metadata.value.columns.forEach((column) => {
    if (column.columnType !== "SECTION") {
      const section = sections.value.find(
        (section) => section.id === column.section
      );
      if (section && column.columnType === "HEADING") {
        const headingColumns = metadata.value.columns.filter(
          (col) =>
            col.heading === column.id &&
            col.id !== column.id &&
            !col.id.startsWith("mg_")
        );

        section.headers.push({
          id: column.id,
          label: column.label,
          type: "HEADING",
          isVisible: computed(() =>
            headingColumns.some((col) => visibilityMap[col.id]?.value === true)
          ),
          isActive: computed(
            () =>
              visibleColumnIds.value.has(column.id) ||
              headingColumns.some((col) => visibleColumnIds.value.has(col.id))
          ),
          errorCount: computed(() => {
            return headingColumns.reduce((acc, col) => {
              if (visibleColumnErrors.value[col.id]) {
                return acc + 1;
              }
              return acc;
            }, 0);
          }),
        });
      }
    }
  });

  const gotoSection = (id: string) => {
    sections.value.forEach((section) => {
      if (section.id === id) {
        scrollTo(id + "-form-field");
      }
      section.headers.forEach((header) => {
        if (header.id === id) {
          scrollTo(id + "-form-field");
        }
      });
    });
  };

  const requiredFields = computed(() => {
    return metadata.value?.columns.filter(
      (column: IColumn) =>
        visibilityMap[column.id]?.value === true &&
        requiredMap[column.id]?.value === true &&
        column.columnType !== "AUTO_ID"
    );
  });

  const emptyRequiredFields = computed(() => {
    return (
      requiredFields.value?.filter((column: IColumn) => {
        if (column.columnType === "BOOL") {
          //boolean required fields are considered filled when they are either true or false
          return (
            formValues.value[column.id] === undefined ||
            formValues.value[column.id] === null
          );
        } else {
          return !formValues.value[column.id];
        }
      }) || []
    );
  });

  const requiredMessage = computed(() => {
    const fieldPlural =
      emptyRequiredFields.value.length > 1 ? "fields" : "field";
    if (emptyRequiredFields.value.length === 0) {
      return "All required fields are filled";
    } else {
      return `${emptyRequiredFields.value.length}/${requiredFields.value.length} required ${fieldPlural} left`;
    }
  });

  const errorMessage = computed(() => {
    const errorCount = Object.values(visibleColumnErrors.value).filter(
      (value) => value !== ""
    ).length;
    const fieldLabel = errorCount === 1 ? "field requires" : "fields require";
    return errorCount > 0
      ? `${errorCount} ${fieldLabel} attention before you can save this ${metadata.value.label}`
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
    if (currentRequiredField.value) {
      scrollTo(`${currentRequiredField.value.id}-form-field`);
    }
  };

  const validateAllColumns = () => {
    errorMap.value = {};
    metadata.value.columns.forEach((column) => {
      validateColumn(column);
    });
  };

  const validateKeyColumns = () => {
    errorMap.value = {};
    const keyColumns = metadata.value.columns.filter((col) => col.key === 1);
    keyColumns.forEach((column) => {
      validateColumn(column);
    });
  };

  const validateColumn = (column: IColumn) => {
    const error = getColumnError(column, formValues.value, metadata.value);
    if (error) {
      errorMap.value[column.id] = error;
    } else {
      delete errorMap.value[column.id];
    }
  };

  const gotoPreviousError = () => {
    const keys = Object.keys(visibleColumnErrors.value);
    if (!keys.length) {
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
      scrollTo(`${previousErrorColumnId}-form-field`);
    }
  };

  const gotoNextError = () => {
    const keys = Object.keys(visibleColumnErrors.value);
    if (!keys.length) {
      return;
    }
    const currentIndex = keys.indexOf(currentErrorField.value?.id ?? "");
    const nextIndex = currentIndex + 1;
    const nextErrorColumnId = keys[nextIndex >= keys.length ? 0 : nextIndex];

    if (nextErrorColumnId) {
      currentErrorField.value = metadata.value.columns.find(
        (col) => col.id === nextErrorColumnId
      );
      scrollTo(`${nextErrorColumnId}-form-field`);
    }
  };

  const insertInto = async () => {
    const formData = toFormData(formValues.value);
    const query = `mutation insert($value:[${metadata.value.id}Input]){insert(${metadata.value.id}:$value){message}}`;
    formData.append("query", query);
    try {
      const res = await $fetch(`/${metadata.value.schemaId}/graphql`, {
        method: "POST",
        body: formData,
      });
      return res;
    } catch (error) {
      await handleFetchError(error, "Error on inserting");
    }
  };

  const updateInto = async () => {
    const formData = toFormData(formValues.value);
    const query = `mutation update($value:[${metadata.value.id}Input]){update(${metadata.value.id}:$value){message}}`;
    formData.append("query", query);
    try {
      const res = await $fetch(`/${metadata.value.schemaId}/graphql`, {
        method: "POST",
        body: formData,
      });
      return res;
    } catch (error) {
      await handleFetchError(error, "Error on updating");
    }
  };

  const deleteRecord = async () => {
    const key = await getPrimaryKey(
      formValues.value,
      metadata.value.id,
      metadata.value.schemaId as string
    );
    const query = `mutation delete($pkey:[${metadata.value.id}Input]){delete(${metadata.value.id}:$pkey){message}}`;
    const variables = { pkey: [key] };

    return $fetch(`/${metadata.value.schemaId}/graphql`, {
      method: "POST",
      body: {
        query,
        variables,
      },
    }).catch((error) =>
      handleFetchError(error, "Error on delete form database")
    );
  };

  const onBlurColumn = (column: IColumn) => {
    validateColumn(column);
  };

  const onUpdateColumn = (column: IColumn) => {
    //only update error map if error already shown so it is removed
    if (errorMap.value[column.id]) {
      validateColumn(column);
    }
  };

  const visibleColumnIds = ref<Set<string>>(new Set<string>());

  const onViewColumn = (column: IColumn) => {
    visibleColumnIds.value.add(column.id);
  };

  const onLeaveViewColumn = (column: IColumn) => {
    visibleColumnIds.value.delete(column.id);
  };

  const visibleColumns = computed(() => {
    return (
      metadata.value?.columns
        .filter((column) => !column.id.startsWith("mg_"))
        .filter((column) => visibilityMap[column.id]?.value === true)
        // .filter((column) => currentSection.value === column.section)
        // only show AUTO_ID columns when they have a value
        .filter(
          (column) =>
            column.columnType !== "AUTO_ID" ||
            formValues.value[column.id] !== undefined
        )
    );
  });

  // reactive intersection of visible columns and error columns
  const visibleColumnErrors = computed(() => {
    const visibleColIds = visibleColumns.value.map((col) => col.id);
    const visibleErrors = Object.entries(errorMap.value).filter(([key]) =>
      visibleColIds.includes(key)
    );
    return Object.fromEntries(visibleErrors);
  });

  const currentSection = computed(() => {
    const activeSections = sections.value.filter((s) => s.isActive.value);
    if (activeSections.length < 1) {
      return sections.value[0]?.id || null;
    } else {
      return activeSections[0]?.id || null;
    }
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
    }
    // if we dont suspect a session timeout, rethrow the original error
    throw error;
  }

  function scrollTo(elementId: string) {
    lastScrollTo.value = elementId;
    const container = document.getElementById(scrollContainerId);

    //lazy scroll, might need to wait for elements to be mounted first
    function attemptScroll() {
      if (container && elementId === "mg_top_of_form-form-field") {
        container.scrollTo({
          top: 0,
          behavior: "smooth",
        });
      } else {
        const target = document.getElementById(elementId);
        if (container && target) {
          const SCROLL_PADDING = 32;
          const offset =
            target.offsetTop - container.offsetTop - SCROLL_PADDING;
          container.scrollTo({ top: offset, behavior: "smooth" });
        } else {
          // try again on the next frame until the element exists
          requestAnimationFrame(attemptScroll);
        }
      }
    }

    attemptScroll();
  }

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
    onLeaveViewColumn,
    sections,
    currentSection,
    visibleColumns,
    visibleColumnErrors,
    validateAllColumns,
    validateKeyColumns,
    lastScrollTo, //for debug
    visibleColumnIds,
    requiredMap,
  };
}
