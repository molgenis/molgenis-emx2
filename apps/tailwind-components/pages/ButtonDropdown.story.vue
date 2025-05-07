<template>
  <div class="flex gap-10">
    <ButtonDropdown label="basic">
      <section class="flex bg-white p-4 w-52">
        <p>
          Behind every exquisite thing that existed, there was something tragic.
        </p>
      </section>
    </ButtonDropdown>

    <hr />

    <ButtonDropdown label="list">
      <section class="flex bg-white p-4 w-52">
        <InputList
          id="dropdown-list"
          :nodes="[
            {
              name: 'Items 1',
            },
            {
              name: 'Item 2',
            },
            {
              name: 'Item 3',
            },
          ]"
          v-model="selectedNodesNames"
          :inverted="true"
        />
      </section>
    </ButtonDropdown>

    <span v-if="selectedNodesNames.length">{{ selectedNodesNames }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const selectedNodesNames = ref([]);
</script>
