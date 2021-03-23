<template>
    <Spinner v-if="loading" />
    <MessageError v-else-if="graphqlError">
        {{ graphqlError }}
    </MessageError>
    <div v-else>
        <InputCheckbox
            v-model="value"
            label="Choose resource(s)"
            :list="true"
            :options="options"
            @input="emitValue()"
        />
    </div>
</template>

<script>
import { InputCheckbox } from "../ui/index.js";
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
  methods: {
    emitValue() {
      this.$emit("input", this.value);
    },
  },
};
</script>
