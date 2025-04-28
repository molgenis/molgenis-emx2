import { type MaybeRef, type Ref, computed, toRef, ref } from "vue";
import type {
  columnValue,
  ITableMetaData,
  IColumn,
  columnId,
} from "../../metadata-utils/src/types";
import { toFormData } from "../utils/toFormData";
import { getPrimaryKey } from "../utils/getPrimaryKey";

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
  };
}
