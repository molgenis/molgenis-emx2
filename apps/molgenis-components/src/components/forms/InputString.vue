<template>
  <FormGroup :id="id" :label="label" :description="description">
    <InputGroup>
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <input
        :id="id"
        :ref="id"
        :name="name"
        :value="value"
        @input="$emit('input', $event.target.value)"
        type="text"
        class="form-control"
        :aria-describedby="id"
        :placeholder="placeholderValue"
      />

      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  name: "InputString",
  components: { FormGroup, InputGroup },
  extends: BaseInput,
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
    You typed: {{ JSON.stringify(value) }}
    <b>Readonly</b>
    <InputString id="input-string2" label="test" :readonly="true" value="can't change me"
                 description="Should not be able to edit this"/>
    <b>In place some</b>
    <InputString id="input-string3" label="test" v-model="value" :inplace="true"
                 description="Should be able to edit in place"/>
    text.<br/>
    value: {{ value }}
    <b>column</b>
    <InputString id="input-string4" :label.sync="column.label" v-model="column.value" :editMeta="true"
                 :description.sync="column.description"/>
    text.<br/>
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
