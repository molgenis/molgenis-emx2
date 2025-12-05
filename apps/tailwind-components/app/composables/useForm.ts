import {
  computed,
  ref,
  watch,
  type MaybeRef,
  unref,
  isRef,
  type ComputedRef,
  type Ref,
} from "vue";
import type {
  columnValue,
  ITableMetaData,
  IColumn,
  columnId,
  LegendSection,
} from "../../../metadata-utils/src/types";
import { toFormData } from "../../../metadata-utils/src/toFormData";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import {
  getColumnError,
  isRequired,
} from "../../../molgenis-components/src/components/forms/formUtils/formUtils";
import { useSession } from "#imports";
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

  // setup visibility signals
  const visibilityMap = metadata.value.columns.reduce((acc, column) => {
    const cleanExpression = column.visible?.replaceAll('"', "'") || "true";

    const exprString = column.visible;
    const params: string[] = [];

    for (const key of formValueKeys) {
      // regex finds formValue keys used inside the expression
      const regex = new RegExp(`(?<!['"])\\b${key}\\b(?!['"])`, "g");
      if (regex.test(exprString || "")) {
        params.push(key);
      }
    }

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
            if (errorMap.value[col.id]) {
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
        const columns = metadata.value.columns.filter(
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
            columns.some((col) => visibilityMap[col.id]?.value === true)
          ),
          isActive: computed(() => visibleColumnIds.value.has(column.id)),
          errorCount: computed(() => {
            return columns.reduce((acc, col) => {
              if (errorMap.value[col.id]) {
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
        isRequired(column.required) &&
        column.columnType !== "AUTO_ID"
    );
  });

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

  const gotoPreviousError = () => {
    const keys = Object.keys(errorMap.value);
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
    const keys = Object.keys(errorMap.value);
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
    throw new Error(message, error);
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
    errorMap,
    validateAllColumns,
    lastScrollTo, //for debug
    visibleColumnIds,
  };
}
