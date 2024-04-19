<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    tableId: string;
    modelValue: { name: string }[];
    nameField?: string;
    keyField?: string;
    descriptionField?: string;
  }>(),
  {
    nameField: "name",
    keyField: "id",
    descriptionField: undefined,
  }
);

const query = `
    query 
    ${props.tableId}( $filter:${props.tableId}Filter )
    {   
        ${props.tableId}( filter:$filter, limit:100000,  offset:0, orderby:{${props.nameField}: ASC} )
        {          
            ${props.keyField} ${props.nameField} ${props.descriptionField}
        }       
        ${props.tableId}_agg( filter:$filter ) { count }
        }
    `;

const options: IOption[] = (
  await fetchGql<any>(query).catch((e) => {
    console.error(e);
  })
)?.data[props.tableId]?.map((respItem: any) => {
  const selectedKeyValues = props.modelValue.map(
    (selectedItem) => selectedItem[props.keyField]
  );
  return {
    id: respItem[props.keyField],
    name: respItem[props.nameField],
    description: props.descriptionField
      ? respItem[props.descriptionField]
      : undefined,
    selected: selectedKeyValues.includes(respItem[props.keyField]),
  };
});

const emit = defineEmits(["update:modelValue"]);
watch(() => props.modelValue, updateSelection, { deep: true });

function updateSelection() {
  options.forEach((option) => {
    const selectedKeyValues = props.modelValue.map(
      (selectedItem) => selectedItem[props.keyField]
    );
    option.selected = selectedKeyValues.includes(option.id);
  });
}

function toggleSelect(option: IOption) {
  if (option.selected) {
    // remove from selection
    emit(
      "update:modelValue",
      props.modelValue.filter((item) => item[props.keyField] !== option.id)
    );
  } else {
    // add to selection
    emit("update:modelValue", [
      ...props.modelValue,
      { [props.keyField]: option.id },
    ]);
  }
}

const selectedNodesNames = computed({
  get() {
    return props.modelValue ? props.modelValue.map((n) => n.name) : [];
  },
  set(newValue) {
    // transform the names back to the original data structure for use in gql query
    const newConditions = newValue.map((name) => ({ name: name }));
    emit("update:modelValue", newConditions);
  },
});
</script>
<template>
  <InputList
    :nodes="options"
    v-model="selectedNodesNames"
    :inverted="mobileDisplay"
  >
  </InputList>
</template>
