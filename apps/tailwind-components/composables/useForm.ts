import type {
  columnValue,
  ITableMetaData,
  IColumn,
  columnId,
} from "../../metadata-utils/src/types";

export default function useForm(
  metadata: MaybeRef<ITableMetaData>,
  formValues: MaybeRef<Record<columnId, columnValue>>,
  errorMap: Ref<Record<columnId, string>>,
  scrollTo: (id: string) => void
) {
  const requiredFields = computed(() => {
    return toRef(metadata).value.columns.filter(
      (column: IColumn) => column.required
    );
  });

  const emptyRequiredFields = computed(() => {
    return requiredFields.value.filter(
      (column: IColumn) => !toRef(formValues).value[column.id]
    );
  });

  const requiredMessage = computed(() => {
    const fieldPrural =
      emptyRequiredFields.value.length > 1 ? "fields" : "field";
    if (emptyRequiredFields.value.length === 0) {
      return "All required fields are filled";
    }
    return `${emptyRequiredFields.value.length}/${requiredFields.value.length} required ${fieldPrural} left`;
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

  return {
    requiredFields,
    emptyRequiredFields,
    requiredMessage,
    errorMessage,
    gotoNextRequiredField,
    gotoPreviousRequiredField,
    gotoNextError,
    gotoPreviousError,
  };
}
