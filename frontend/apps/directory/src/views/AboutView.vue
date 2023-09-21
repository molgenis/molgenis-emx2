<template>
  <div class="about">
    <CheckboxFilter :filterName="'TestFilter'" :options="options" />
  </div>
</template>

<script>
import CheckboxFilter from "../components/filters/CheckboxFilter.vue";

export default {
  components: {
    CheckboxFilter,
  },
  data() {
    return {
      options: () =>
        new Promise((resolve) =>
          resolve([
            { text: "Option 1", value: 1 },
            { text: "Option 2", value: 2 },
            { text: "Option 3", value: 3 },
          ])
        ),
    };
  },
};
</script>
