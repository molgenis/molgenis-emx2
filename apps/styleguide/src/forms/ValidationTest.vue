<template>
  <div>
    <InputText v-model="valueString" label="value" />
    <div>
      {{ valueError }}
      {{ value }}
    </div>
    <InputText v-model="expression" label="expression" />
    <div class="form-group">
      <label><b>Validation result</b></label>
      <MessageError v-if="expressionError">
        {{ expressionError }}
      </MessageError>
      {{ valid }}
    </div>
    <label>variables:</label>
    <li v-for="variable in variableNames" :key="variable">
      {{ variable }}
    </li>
  </div>
</template>
<script>
import InputText from "./InputText";
import MessageError from "./MessageError";
import { Expressions } from "@molgenis/expressions";

export default {
  components: {
    InputText,
    MessageError,
  },
  data() {
    return {
      expression: "{a} notempty",
      expressionError: null,
      valueString: '{ "a": "b" }',
      valueError: null,
      variableNames: [],
      valid: null,
      value: null,
    };
  },
  methods: {
    evaluate() {
      this.value = null;
      //check value
      try {
        this.value = JSON.parse(this.valueString);
        this.valueError = null;
        this.variableNames = Expressions.variableNames(this.expression);
      } catch (error) {
        this.valueError = error.toString();
      }
      try {
        this.expressionError = null;
        this.valid = Expressions.evaluate(this.expression, this.value);
      } catch (error) {
        this.expressionError = error.toString;
      }
    },
  },
  watch: {
    valueString() {
      this.evaluate();
    },
    expression() {
      this.evaluate();
    },
  },
  created() {
    this.evaluate();
  },
};
</script>

<docs>
```
<ValidationTest/>
```
</docs>
