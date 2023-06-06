<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    tableName: string;
    nameField?: string;
    keyField?: string;
    descriptionField?: string;
    isMultiSelect?: boolean;
    modelValue: [];
  }>(),
  {
    isMultiSelect: true,
    nameField: "name",
    keyField: "id",
    descriptionField: undefined,
  }
);

interface IOption {
  id: string;
  name: string;
  description?: string;
  selected: boolean;
}

const query = `
    query 
    ${props.tableName}( $filter:${props.tableName}Filter )
    {   
        ${props.tableName}( filter:$filter, limit:100000,  offset:0, orderby:{${props.nameField}: ASC} )  
        {          
            ${props.keyField} ${props.nameField} ${props.descriptionField}
        }       
        ${props.tableName}_agg( filter:$filter ) { count }
        }
    `;

const options: IOption[] = (
  await fetchGql(query).catch(e => {
    console.error(e);
  })
)?.data[props.tableName]?.map(respItem => {
  const selectedKeyValues = props.modelValue.map(
    selectedItem => selectedItem[props.keyField]
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
  options.forEach(option => {
    const selectedKeyValues = props.modelValue.map(
      selectedItem => selectedItem[props.keyField]
    );
    option.selected = selectedKeyValues.includes(option.id);
  });
}

function toggleSelect(option: IOption) {
  if (option.selected) {
    // remove from selection
    emit(
      "update:modelValue",
      props.modelValue.filter(item => item[props.keyField] !== option.id)
    );
  } else {
    // add to selection
    emit("update:modelValue", [
      ...props.modelValue,
      { [props.keyField]: option.id },
    ]);
  }
}
</script>
<template>
  <ul>
    <li v-for="option in options" :key="option.name" class="mb-2.5">
      <div class="flex items-start">
        <span
          class="flex items-center justify-center w-6 h-6 rounded-full text-search-filter-group-toggle hover:bg-search-filter-group-toggle hover:cursor-pointer">
        </span>
        <div class="flex items-center">
          <input
            type="checkbox"
            :id="option.name"
            :name="option.name"
            :checked="option.selected"
            @click.stop="toggleSelect(option)"
            :class="{ 'text-search-filter-group-checkbox': option.selected }"
            class="w-5 h-5 rounded-3px ml-[6px] mr-2.5 mt-0.5 border border-checkbox" />
        </div>
        <label
          :for="option.name"
          class="hover:cursor-pointer text-body-sm group">
          <span class="group-hover:underline">{{ option.name }}</span>
          <div class="inline-flex items-center whitespace-nowrap">
            <!--
            <span
              v-if="option?.result?.count"
              class="inline-block mr-2 text-blue-200 group-hover:underline decoration-blue-200 fill-black"
              hoverColor="white"
              >&nbsp;- {{ option.result.count }}
            </span>
            -->
            <div class="inline-block">
              <CustomTooltip
                v-if="option.description"
                label="Read more"
                hoverColor="white"
                :content="option.description" />
            </div>
          </div>
        </label>
      </div>
    </li>
  </ul>
</template>
