<template>
  <CardItem>
    <template #title>
      <h5>{{ biobank.name }}</h5>
    </template>

    <small>
      <ViewGenerator :viewmodel="{ attributes }" />
      <MatchesOn :viewmodel="biobank" />
      <router-link
        :to="'/biobank/' + biobank.id"
        :title="`${biobank.name} details`"
        class="text-info ml-1"
      >
        <span>More details</span>
      </router-link>
    </small>
  </CardItem>
</template>

<script setup lang="ts">
import { getViewmodel } from "../../functions/viewmodelMapper";
import { IBiobanks } from "../../interfaces/directory";
import { useSettingsStore } from "../../stores/settingsStore";
import CardItem from "../CardItem.vue";
import ViewGenerator from "../generators/ViewGenerator.vue";
import MatchesOn from "./MatchesOn.vue";
import { computed } from "vue";

const props = defineProps<{
  biobank: IBiobanks;
}>();

const attributes = computed(() => {
  const viewmodel = getViewmodel(
    props.biobank,
    useSettingsStore().config.biobankColumns
  );
  const columns = useSettingsStore().config.biobankColumns;
  return columns
    .filter((item) => item.showOnBiobankCard)
    .map((item) =>
      viewmodel.attributes.find(
        (vm: { label: string | undefined }) => vm.label === item.label
      )
    );
});
</script>
