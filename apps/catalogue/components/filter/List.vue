<script setup lang="ts">
import { fetchGql } from "../../composables/fetchGql";
import type { INode } from "../../../tailwind-components/types/types";
import type { IFilterCondition, optionsFetchFn } from "../../interfaces/types";
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    tableId: string;
    modelValue: IFilterCondition[];
    nameField?: string;
    descriptionField?: string;
    mobileDisplay?: boolean;
    options?: INode[] | optionsFetchFn;
  }>(),
  {
    nameField: "name",
    descriptionField: undefined,
    mobileDisplay: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

const query = ` query 
    ${props.tableId}( $filter:${props.tableId}Filter )
    {   
        ${props.tableId}( filter:$filter, limit:100000,  offset:0, orderby:{${props.nameField}: ASC} )
        {          
             ${props.nameField} ${props.descriptionField}
        }       
        ${props.tableId}_agg( filter:$filter ) { count }
        }
    `;

const nodes = props.options
  ? typeof props.options === "function"
    ? await props.options()
    : props.options
  : (await fetchGql<INode>(query)).data[props.tableId].map(dataToNode);

function dataToNode(respObject: any): INode {
  return {
    name: respObject[props.nameField],
    description: props.descriptionField
      ? respObject[props.descriptionField]
      : undefined,
  };
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
    :nodes="nodes"
    v-model="(selectedNodesNames as string[])"
    :inverted="mobileDisplay"
  >
  </InputList>
</template>
