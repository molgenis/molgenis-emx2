<script setup lang="ts">
import { ref, type Ref } from "vue";

const title = ref("My list");
const columnCount = ref(1);
const type: Ref<"standard" | "link"> = ref("standard");

const numberOfDemoItems = ref(3);
</script>

<template>
  <div class="flex">
    <div class="flex-1">
      <DisplayList :title="title" :columnCount="columnCount" :type="type">
        <DisplayListItem v-for="index in numberOfDemoItems"
          >Item {{ index }}</DisplayListItem
        >
      </DisplayList>
    </div>

    <div class="h-12 ml-4 mt-2">
      <fieldset class="border border-gray-900 mb-2">
        <legend class="m-2 px-2">Props</legend>
        <div class="mb-2">
          <label
            class="ml-1 text-title hover:cursor-pointer"
            for="display-list-title"
          >
            title
          </label>
          <input
            id="display-list-title"
            class="ml-2 text-title hover:cursor-pointer"
            type="text"
            v-model="title"
          />
        </div>
        <div class="mb-2">
          <label
            class="ml-1 text-title hover:cursor-pointer"
            for="display-list-column-count"
          >
            columnCount
          </label>
          <input
            id="display-list-column-count"
            class="ml-2 text-title hover:cursor-pointer"
            type="number"
            min="0"
            max="3"
            v-model="columnCount"
          />
        </div>
        <div class="mb-2 ml-1">
          <input
            type="radio"
            id="display-list-type-standard"
            value="standard"
            v-model="type"
          />
          <label class="mr-3 text-title" for="display-list-type-standard"
            >standard</label
          >

          <input
            type="radio"
            id="display-list-type-link"
            value="link"
            v-model="type"
          />
          <label class="text-title" for="display-list-type-link">link</label>
        </div>
      </fieldset>

      <hr />

      <div class="mb-2 text-title">
        Number of demo items:
        <input type="number" v-model="numberOfDemoItems" />
      </div>
    </div>
  </div>
</template>
