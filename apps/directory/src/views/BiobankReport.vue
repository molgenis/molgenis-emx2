<template>
  <div class="mg-biobank-card container pb-4">
    <!-- TODO: replace DisplayValues with View Generator-->
    <DisplayValues
      v-if="biobank.Biobanks"
      :values="biobank.Biobanks[0]"
      :meta="biobankColumns"
    />
  </div>
</template>

<script>
import { ref } from "vue";
import DisplayValues from "../components/biobankcards-components/DisplayValues.vue";

import { useRoute } from "vue-router";
import { useBiobanksStore } from "../stores/biobanksStore";
import { useSettingsStore } from "../stores/settingsStore";

export default {
  name: "biobank-report-card",
  components: { DisplayValues },
  setup() {
    const settingsStore = useSettingsStore();
    const biobanksStore = useBiobanksStore();

    const biobankColumns = settingsStore.config.biobankColumns;
    console.log(biobankColumns);

    const biobank = ref({});
    const route = useRoute();

    biobanksStore.getBiobankCard(route.params.id).then((result) => {
      biobank.value = result;
      console.log(biobank.value);
    });

    return { settingsStore, biobanksStore, biobank, biobankColumns };
  },
  methods: {},
};
</script>
