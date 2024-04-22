<script setup lang="ts">
import type { INode } from "../../../tailwind-components/types/types";
import type { IFilterCondition } from "~/interfaces/types";

const props = withDefaults(
  defineProps<{
    tableId: string;
    modelValue: IFilterCondition[];
    nameField?: string;
    descriptionField?: string;
    mobileDisplay?: boolean;
    optionsQuery?: string;
    optionsRespResolver?: (respObject: any) => INode[];
  }>(),
  {
    nameField: "name",
    descriptionField: undefined,
    mobileDisplay: false,
  }
);

const emit = defineEmits(["update:modelValue"]);

const query =
  props.optionsQuery ||
  `
    query 
    ${props.tableId}( $filter:${props.tableId}Filter )
    {   
        ${props.tableId}( filter:$filter, limit:100000,  offset:0, orderby:{${props.nameField}: ASC} )
        {          
             ${props.nameField} ${props.descriptionField}
        }       
        ${props.tableId}_agg( filter:$filter ) { count }
        }
    `;

const optionsResp = await fetchGql<INode>(query);

const nodes = props.optionsRespResolver
  ? // use the resolver pased via the config
    props.optionsRespResolver(optionsResp.data)
  : // use the default resolver
    optionsResp.data[props.tableId].map(dataToNode);

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
    v-model="selectedNodesNames"
    :inverted="mobileDisplay"
  >
  </InputList>
</template>
