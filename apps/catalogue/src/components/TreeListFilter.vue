<template>
  <div>
    <IconAction :icon="expand ? 'caret-up' : 'caret-down'" @click="toggle" />
    <strong class="font-weight-bold">{{ value.name }}</strong>
    <div v-if="expand">
      <Spinner v-if="loading" />
      <div v-else class="ml-4" v-for="option in value.options">
        <input type="checkbox" v-model="option.checked" />
        {{ option[value.refColumn] }}
      </div>
    </div>
  </div>
</template>

<script>
import { IconAction, InputCheckbox, Spinner } from "@mswertz/emx2-styleguide";
import { request } from "graphql-request";

export default {
  components: { IconAction, InputCheckbox, Spinner },
  props: {
    value: Object,
  },
  data() {
    return {
      expand: false,
      loading: false,
    };
  },
  computed: {
    active() {},
  },
  methods: {
    toggle() {
      if (this.value.options == undefined) {
        this.loading = true;
        request(
          "graphql",
          "{" + this.value.refTable + "{" + this.value.refColumn + "}}"
        )
          .then((data) => {
            this.value.options = data[this.value.refTable];
          })
          .catch((error) => {
            this.graphqlError = error.response.errors[0].message;
          })
          .finally(() => {
            this.loading = false;
          });
      }
      this.expand = !this.expand;
    },
  },
};
</script>
