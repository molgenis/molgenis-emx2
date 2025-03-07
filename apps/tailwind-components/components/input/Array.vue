<template>
  <div>
    <div v-for="(value, index) in valuesNotUndefined" :key="index" class="flex items-center gap-1">
      <Input
        :id="id+'_'+index"
        :modelValue="valuesNotUndefined[index]"
        v-bind="partialProps($props)"
        :type="nonArrayType(props.type || 'STRING_ARRAY')"
        @blur="emit('blur')"
        @focus="emit('focus')"
        @update:model-value="emit('update:modelValue',setValues($event,index))"
      />
      <Button iconOnly type="secondary" icon="trash" label="Remove item"  v-if="valuesNotUndefined.length > 1" @click="clearInput(valuesNotUndefined, index)" />
    </div>
    <Button type="secondary" size="small" icon="plus" label="Add a additional item" @click="addItem(valuesNotUndefined)"/>
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "~/types/types";
import type { CellValueType } from "../../../metadata-utils/src/types";

let props = defineProps<
  IInputProps & {
    modelValue: any[]|undefined;
    type?: string;
  }
>();

const values = ref(props.modelValue);
const emit = defineEmits(["focus", "blur", "update:modelValue"]);

function setValues(value:any, index:number){
  if(values.value === undefined){
    values.value =[undefined]
  }else{
    values.value[index] = value;
  }
  return values.value;
} 

const valuesNotUndefined = computed(()=>{
  if(values.value === undefined){
    values.value =[undefined]
  }
  return values.value;
})

function addItem(values: any) {
  values.push(undefined);
  emit("update:modelValue", values);
}

function clearInput(values: any, index: number) {
  if (values.length > 1) {
    values.splice(index, 1);
  }
  emit("update:modelValue", values);
}

function partialProps(props: any): { [key: string]: string } {
  let clone = { ...props };
  delete clone.id;
  delete clone.type;
  delete clone.modelValue;
  return clone;
}

function nonArrayType(type: string): CellValueType {
  return ({
    BOOL_ARRAY: "BOOL",
    DATE_ARRAY: "DATE",
    DATETIME_ARRAY: "DATETIME",
    DECIMAL_ARRAY: "DECIMAL",
    PERIOD_ARRAY: "PERIOD",
    EMAIL_ARRAY: "EMAIL",
    HYPERLINK_ARRAY: "HYPERLINK",
    INT_ARRAY: "INT",
    LONG_ARRAY: "LONG",
    STRING_ARRAY: "STRING",
    TEXT_ARRAY: "TEXT",
    UUID_ARRAY: "UUID",
  }[type] || "STRING") as CellValueType;
}
</script>
