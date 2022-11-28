<template>
  <Spinner v-if="loading" />
  <MessageError v-else-if="graphqlError">{{ graphqlError }}</MessageError>
  <div v-else>
    <InputCheckbox
      label="Choose resource(s)"
      :options="options"
      v-model="value"
      :list="true"
      @update:modelValue="emitValue()"
    />
  </div>
</template>

<script>
import { InputCheckbox } from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    InputCheckbox,
  },
  data() {
    return {
      value: [],
      options: [],
      loading: false,
      graphqlError: null,
    };
  },
  methods: {
    emitValue() {
      this.$emit("update:modelValue", this.value);
    },
  },
  created() {
    request("graphql", "{Collections{name}}")
      .then((data) => {
        this.options = data.Collections.map((c) => c.name);
      })
      .catch((error) => {
        this.graphqlError = error.response.errors[0].message;
      })
      .finally(() => {
        this.loading = false;
      });
  },
};
</script>
