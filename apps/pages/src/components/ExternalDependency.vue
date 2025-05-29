<template>
  <!-- url: {{ url}}
  defer: {{ defer }} -->
  <div class="d-flex flex-row flex-wrap">
    <div class="flex-fill mr-4">
      <label for="javascript-urls"
        >Enter the URL to javascript dependency</label
      >
      <InputHyperlink id="javascript-urls" v-model="url" @change="updated" />
    </div>
    <div>
      <label for="javascript-defer">Defer dependency?</label>
      <InputBoolean
        id="javascript-defer"
        v-model="defer"
        :isClearable="false"
        @change="updated"
      />
    </div>
  </div>
</template>

<script>
import { InputBoolean, InputHyperlink } from "molgenis-components";

export default {
  components: {
    InputHyperlink,
    InputBoolean,
  },
  data() {
    return {
      url: null,
      defer: false,
    };
  },
  emits: ["update:modelValue"],
  methods: {
    updated() {
      this.$emit("update:modelValue", { url: this.url, defer: this.defer });
    },
  },
  watch: {
    url: {
      handler() {
        this.updated();
      },
    },
  },
};
</script>
