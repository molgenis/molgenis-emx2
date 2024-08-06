<template>
  <div class="flex flex-row  gap-1">
        <Button  v-for="type in types" :type="type" :size="activeSize" :iconPosition="activeIconPosition" :icon="showIcon?'info':''"> Button {{type}} </Button>
  </div>

  <div class="h-12 ml-4 mt-2">
        <fieldset class="border border-gray-900 mb-2">
          <legend class="m-2 px-2">Props</legend>
          <div class="mb-2 ml-1">
            <label class="mr-3" for="display-list-type-standard">Sizes</label>
            <select
              class="form-control"
              v-model="activeSize"
              required
              id="display-list-type-standard"
            >
              <option v-for="size in sizes" :id="`option-type-${size}`">
                {{ size }}
              </option>
            </select>
          </div>
          <div class="mb-2 ml-1">
            <input id="show-icon" type="checkbox" v-model="showIcon" />
            <label class="ml-1" for="show-icon">Show icon</label>
          </div>
          <div class="mb-2 ml-1">
            <label class="mr-3" for="display-list-type-standard">Icon position</label>
            <select
              class="form-control"
              v-model="activeIconPosition"
              required
              id="display-list-type-standard"
            >
              <option v-for="iconPosition in iconPositions" :id="`option-type-${iconPosition}`">
                {{ iconPosition }}
              </option>
            </select>
          </div>
        </fieldset>
    </div>
</template>

<script setup lang="ts">
const types = [
  "primary",
  "secondary",
  "tertiary",
  "outline",
  "disabled",
  "filterWell",
];
const sizes = [
  "tiny","small","medium","large"
]
const iconPositions = ["left", "right"]
const activeSize: string = ref(sizes[1]);
const activeIconPosition: string = ref(iconPositions[0]);
const showIcon = ref(false);
</script>
