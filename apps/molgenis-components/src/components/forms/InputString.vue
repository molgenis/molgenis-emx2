<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="stringError">
    <InputGroup>
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <input
        :id="id"
        :ref="id"
        :name="name"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        type="text"
        class="form-control"
        :class="{ 'is-invalid': stringError }"
        :aria-describedby="id"
        :placeholder="placeholderValue"
        :readonly="readonly" />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  name: "InputString",
  components: { FormGroup, InputGroup },
  extends: BaseInput,
  props: {
    stringLength: {
      type: Number,
      default: 255,
    },
  },
  computed: {
    stringError() {
      if (
        this.modelValue &&
        this.modelValue.length &&
        this.modelValue.length > this.stringLength
      ) {
        return `Please limit to ${this.stringLength} characters.`;
      } else {
        return this.errorMessage;
      }
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>

<docs>
<template>
  <div>
    <InputString id="input-string1" v-model="value" label="My string input label" description="Some help needed?"/>
    You typed: {{ JSON.stringify(value) }}<br/>
    <b>Readonly</b>
    <InputString id="input-string2" label="test" :readonly="true" value="can't change me"
                 description="Should not be able to edit this"/>
    <b>column</b>
    <InputString id="input-string4" :label.sync="column.label" v-model="column.value" :editMeta="true"
                 :description.sync="column.description"/>
    text.<br/><br/>
    <InputString id="input-string5" v-model="value" :stringLength="4" label="maximum stringLength (4)"/>
    <b>Readonly</b>

    column :
    <pre>{{ column }}</pre>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "blaat",
        column: {value: null, label: 'testlabel', description: 'test description'}
      };
    }
  };
</script>
</docs>
