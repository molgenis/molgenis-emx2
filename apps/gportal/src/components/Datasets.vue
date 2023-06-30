<template>
  <RoutedTableExplorer
    tableName="Dataset"
    :showColumns="['id', 'title', 'description', 'publisher', 'distribution']"
    :canEdit="false"
    :canManage="false"
  >
    <template v-slot:rowheader="slotProps">
      <div class="checkbox">
        <input
          :id="slotProps.row.id"
          type="checkbox"
          class="input"
          name="rems-selections"
          v-model="selection"
          :value="slotProps.row.id"
          :ref="setRefs"
          @change="updateRowStyling"
        />
        <label :for="slotProps.row.id" class="label visually-hidden">
          <span>Select dataset</span>
        </label>
      </div>
    </template>
  </RoutedTableExplorer>
  <div class="d-flex flex-row justify-content-end">
    <ButtonAlt @click="clearAll"> Clear all </ButtonAlt>
    <ButtonOutline @click="selectAll"> Select all </ButtonOutline>
    <a :href="url" type="button" class="btn btn-primary mx-2">
      Request Access {{ selection.length ? `(${selection.length})` : "" }}
      <ExternalLink />
    </a>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import {
  RoutedTableExplorer,
  ButtonAlt,
  ButtonOutline,
} from "molgenis-components";

import ExternalLink from "./icons/external-link.vue";

let selection = ref([]);
let checkboxes = ref([]);
let url = ref("https://rems-gdi-nl.molgenis.net");

watch([selection], setUrl);

function setUrl() {
  const resources = selection.value.map((item) => `resource=${item}`);
  url = `https://rems-gdi-nl.molgenis.net/apply-for?${resources.join("&")}`;
}

function clearAll() {
  selection.value = [];
  url = null;
}

function setRefs(value) {
  if (value !== null) {
    checkboxes.value.push(value._value);
  }
}

function selectAll() {
  checkboxes.value.forEach((value) => {
    if (selection.value.indexOf(value) === -1) {
      selection.value.push(value);
    }
  });
  setUrl();
}

onMounted(() => {
  setTimeout(() => {
    const colheader = document.querySelector("table.table > thead > th > h6");
    colheader.classList = "";
    colheader.classList.add("mb-0", "align-text-bottom", "text-nowrap");
    colheader.innerText = "Select";
  }, 200);
});
</script>

<style lang="scss">
.heroicons.external-link {
  $size: 16px;
  width: $size;
  height: $size;
  margin-top: -4px;
  stroke-width: 2;
}
</style>
