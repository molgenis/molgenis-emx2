<template>
  <div>
    <RoutedTableExplorer
      tableName="Dataset"
      :showColumns="['id', 'title', 'description', 'publisher', 'distribution']"
      :canEdit="false"
      :canManage="false"
    >
      <template v-slot:rowheader="slotProps">
        <ButtonAction
          type="button"
          @click="accessRequest = `https://host/${slotProps.row.id}`"
          >Request</ButtonAction
        >
      </template>
    </RoutedTableExplorer>
    <p>Your request:</p>
    <output class="d-block p-2 bg-dark text-light">
      {{ accessRequest ? accessRequest : "Select a dataset" }}
    </output>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { RoutedTableExplorer, ButtonAction } from "molgenis-components";

let accessRequest = ref(null);
</script>
