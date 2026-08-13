<script setup lang="ts">
import { ref } from "vue";
import RowControls from "../../components/table/control/RowControls.vue";
import Checkbox from "../../components/input/Checkbox.vue";
const canEdit = ref(true);
const allRowsSelected = ref(false);
const numberOfSelectedRows = ref(0);
</script>
<template>
  <div class="flex gap-4 pt-5">
    <div class="w-2/3">
      <RowControls
        :number-of-selected-rows="numberOfSelectedRows"
        :all-rows-selected="allRowsSelected"
        :can-edit="canEdit"
        @rowAction="(payload) => console.log('Row action emitted:', payload)"
      />
    </div>
    <div class="w-1/3">
      <fieldset class="border border-form rounded-theme p-4">
        <legend>Prop settings</legend>
        <div class="flex items-center gap-2">
          <label for="can-edit-input">
            <span class="text-title">Can edit</span>
          </label>
          <Checkbox id="can-edit-input" v-model="canEdit" />
        </div>
        <div class="flex items-center gap-2">
          <label for="all-rows-selected-input">
            <span class="text-title">All rows selected</span>
          </label>
          <Checkbox id="all-rows-selected-input" v-model="allRowsSelected" />
        </div>
        <div class="flex items-center gap-2">
          <label for="number-of-selected-rows-input">
            <span class="text-title">Number of selected rows</span>
          </label>
          <input
            id="number-of-selected-rows-input"
            type="number"
            min="0"
            max="100"
            v-model.number="numberOfSelectedRows"
            class="border border-form rounded-theme p-1 w-20"
          />
        </div>
      </fieldset>
    </div>
  </div>
</template>
