<template>
  <FormGroup :id="id + '-0'" :label="label" :required="required" description="description" :errorMessage="errorMessage">
    <div v-for="(value, index) in values" :key="index">
      <component
        :is="inputType"
        :id="id + '-' + index"
        :modelValue="values[index]"
        :showAddButton="index === values.length"
        @update:modelValue="handleUpdate($event, index)"
      >
        <template v-slot:append>
          <button v-if="values.length > 1" @click="clearInput(index)" class="btn btn-outline-primary" type="button">
            <i class="fas fa-fw fa-times"></i>
          </button>
          <button @click="addItem(index)" class="btn btn-outline-primary" type="button">
            <i class="fas fa-fw fa-plus"></i>
          </button>
        </template>
      </component>
    </div>
  </FormGroup>
</template>

<script>
import FormGroup from "./FormGroup.vue";
import InputBoolean from "./InputBoolean.vue";
import InputDate from "./InputDate.vue";
import InputDateTime from "./InputDateTime.vue";
import InputDecimal from "./InputDecimal.vue";
import InputInt from "./InputInt.vue";
import InputLong from "./InputLong.vue";
import InputString from "./InputString.vue";
import InputText from "./InputText.vue";
import BaseInput from "./baseInputs/BaseInput.vue";

export default {
  name: "ArrayInput",
  extends: BaseInput,
  components: { FormGroup },
  data() {
    return { values: this.modelValue || [null] };
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
        BOOL_ARRAY: InputBoolean,
        DATE_ARRAY: InputDate,
        DATETIME_ARRAY: InputDateTime,
        DECIMAL_ARRAY: InputDecimal,
        EMAIL_ARRAY: InputString,
        HYPERLINK_ARRAY: InputString,
        INT_ARRAY: InputInt,
        LONG_ARRAY: InputLong,
        STRING_ARRAY: InputString,
        TEXT_ARRAY: InputText,
        UUID_ARRAY: InputString,
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
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <div>
        <h3><label>String array</label></h3>
        <ArrayInput
          id="array-string"
          columnType="STRING_ARRAY"
          v-model="stringValue"
        />
      </div>
      <div>
        {{ stringValue }}
      </div> 
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Integer array</label></h3>
        <ArrayInput
          id="array-integer"
          columnType="INT_ARRAY"
          v-model="intValue"
        />
      </div>
      <div>
        {{ intValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Decimal array</label></h3>
        <ArrayInput
          id="array-decimal"
          columnType="DECIMAL_ARRAY"
          v-model="decimalValue"
        />
      </div>
      <div>
        {{ decimalValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Long array</label></h3>
        <ArrayInput
          id="long-array"
          columnType="LONG_ARRAY"
          v-model="longValue"
        />
      </div>
      <div>
        {{ longValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Text array</label></h3>
        <ArrayInput
          id="text-array"
          columnType="TEXT_ARRAY"
          v-model="textValue"
        />
      </div>
      <div>
        {{ textValue }}
      </div>
    </DemoItem>    
    <DemoItem>
      <div>
        <h3><label>Date array</label></h3>
        <ArrayInput
          id="date-array"
          columnType="DATE_ARRAY"
          v-model="dateValue"
        />
      </div>
      <div>
        {{ dateValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Date Time array</label></h3>
        <ArrayInput
          id="date-time-array"
          columnType="DATETIME_ARRAY"
          v-model="dateTimeValue"
        />
      </div>
      <div>
        {{ dateTimeValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Uuid array</label></h3>
        <ArrayInput
          id="uuid-array"
          columnType="UUID_ARRAY"
          v-model="uuidValue"
        />
      </div>
      <div>
        {{ uuidValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Hyperlink array</label></h3>
        <ArrayInput
          id="hyperlink-array"
          columnType="HYPERLINK_ARRAY"
          v-model="hyperlinkValue"
        />
      </div>
      <div>
        {{ hyperlinkValue }}
      </div>
    </DemoItem>
    <DemoItem>
      <div>
        <h3><label>Email array</label></h3>
        <ArrayInput
          id="email-array"
          columnType="EMAIL_ARRAY"
          v-model="emailValue"
        />
      </div>
      <div>
        {{ emailValue }}
      </div>
    </DemoItem>
  </div>
</template>

<script>
const ipsum =
  `Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
Suscipit tellus mauris a diam maecenas sed. Nulla at volutpat diam ut venenatis tellus.`;

export default {
  methods: {
    alert(text) {
      alert(text);
    },
  },
  data() {
    return {
      stringValue: ["String array value"],
      intValue: [1, 3, 3, 7],
      decimalValue: [3.7, 4.2],
      longValue: ["1234567890123456789"],
      textValue: [ipsum],
      dateValue: ["1970-01-20" ],
      dateTimeValue: ["1970-01-29T01:10:00"],
      uuidValue: ["randomuuid"],
      hyperlinkValue: ["https://molgenis.org"],
      emailValue: ["test@molgenis.org"],
    };
  },
};
</script>
</docs>
