<template>
  <component
    :is="inputType"
    :id="id"
    :value="value"
    :isMultiSelect="isMultiSelect"
    v-bind="$props"
    @input="$emit('input', $event)"
  />
</template>
<script>
import BaseInput from "./BaseInput.vue";
import ArrayInput from "./ArrayInput.vue";
import InputString from "./InputString.vue";
import InputOntology from "./InputOntology.vue";

export default {
  name: "FormInput",
  extends: BaseInput,
  props: {
    /* to pass options hardcode */
    options: Array,
  },
  computed: {
    isMultiSelect() {
      return this.type === "ONTOLOGY_ARRAY";
    },
    inputType() {
      return {
        STRING: InputString,
        STRING_ARRAY: ArrayInput,
        ONTOLOGY: InputOntology,
        ONTOLOGY_ARRAY: InputOntology,
      }[this.type];
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div>
      <FormInput
          id="form-input-string"
          type="STRING"
          v-model="stringValue"
      ></FormInput>
    </div>
    <div>
      {{ stringValue }}
    </div>
    <div>
      <FormInput
          id="form-input-stringArray"
          type="STRING_ARRAY"
          v-model="stringArrayValue"
      ></FormInput>
    </div>
    <div>
      {{ stringArrayValue }}
    </div>
    <div>
      <FormInput
          id="form-input-ontology"
          type="ONTOLOGY"
          :options="options"
          v-model="ontologyValue"
      ></FormInput>
    </div>
    <div>
      {{ ontologyValue }}
    </div>
    <div>
      <FormInput
          id="form-input-ontologyArray"
          type="ONTOLOGY_ARRAY"
          :options="options"
          v-model="ontologyArrayValue"
      ></FormInput>
    </div>
    <div>
      {{ ontologyArrayValue }}
    </div>
  </demo-item>
</template>
<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      },
    },
    data() {
      return {
        stringValue: "foo",
        stringArrayValue: ["bar"],
        ontologyValue: "red",
        ontologyArrayValue: ["green"],
        options: [
          {name: "pet"},
          {name: "cat", parent: {name: "pet"}},
          {name: "dog", parent: {name: "pet"}},
          {name: "cattle"},
          {name: "cow", parent: {name: "cattle"}},
        ],
      };
    },
  };
</script>
</docs>
