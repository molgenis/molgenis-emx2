import type {
  columnValue,
  ITableMetaData,
  IColumn,
  columnId,
} from "../../metadata-utils/src/types";

export default function useForm(
  metadata: MaybeRef<ITableMetaData>,
  formValues: Ref<Record<columnId, columnValue>>,
  errorMap: Ref<Record<columnId, string>>
) {
  const requiredFields = computed(() => {
    return toRef(metadata).value.columns.filter(
      (column: IColumn) => column.required
    );
  });

  const emptyRequiredFields = computed(() => {
    return requiredFields.value.filter(
      (column: IColumn) => !formValues.value[column.id]
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

  return {
    requiredFields,
    emptyRequiredFields,
    requiredMessage,
    errorMessage,
  };
}
