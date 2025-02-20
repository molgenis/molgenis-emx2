<template>
  <div v-for="(value, index) in values" :key="index">
    <div>hoi</div>
    <Input
      v-model="values[index]"
      v-bind="$props"
      :type="inputType(props.type||'STRING_ARRAY')"
      @blur="emit('blur')"
      @focus="emit('focus')"
    />

<!--
    <Input
      v-bind="$props"
      @blur="emit('blur')"
      @focus="emit('focus')"
    >
      <template v-slot:append>
        <button
          v-if="values.length > 1"
          @click="clearInput(index)"
          class="btn btn-outline-primary"
          type="button"
        >
          <i class="fas fa-fw fa-times"></i>
        </button>
        <button
          @click="addItem(index)"
          class="btn btn-outline-primary"
          type="button"
        >
          <i class="fas fa-fw fa-plus"></i>
        </button>
      </template>
    </Input>
    -->
  </div>
</template>

<script setup lang="ts">
import type { IInputProps } from "~/types/types";
import type { CellValueType } from "../../../metadata-utils/src/types";
const values = defineModel<string[]>();

let props = defineProps<
  IInputProps & {
    type?: string;
  }
>();
const emit = defineEmits(["focus", "blur", "update:modelValue"]);
function inputType(type:string):CellValueType {
      return (({
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
      }[type])||"STRING") as CellValueType;
    }
</script>
<!--
export default {
  name: "ArrayInput",
  extends: BaseInput,
  components: { FormGroup },
  data() {
    return { values: this.modelValue?.length ? this.modelValue : [null] };
  },
  props: {
    columnType: {
      type: String,
      required: true,
    },
  },
  computed: {
    inputType() {
      return {
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
      }[this.columnType];
    },
  },
  methods: {
    addItem(index) {
      this.values.splice(index + 1, 0, null);
      this.$emit("update:modelValue", this.values);
    },
    clearInput(index) {
      if (this.values.length > 1) {
        this.values.splice(index, 1);
      }
      this.$emit("update:modelValue", this.values);
    },
    handleUpdate(event, index) {
      this.values[index] = event;
      this.$emit("update:modelValue", this.values);
    },
  },
  emits: ["update:modelValue"],
};
-->