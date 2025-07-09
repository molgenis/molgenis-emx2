<script lang="ts" setup>
import { ref, onBeforeMount, watch } from "vue";

const modelValue = ref<string>("");
const listboxData = ref<string[]>([]);
const listboxState = ref<string>("");
const searchTerm = ref<string>("");

type Resp<T> = {
  data: Record<string, T[]>;
};

interface Pet {
  name: string;
}

interface PostBody {
  query: string;
  variables?: { filter: Record<string, any> };
}

async function getExampleData() {
  const body: PostBody = {
    query: `query (\$filter: PetFilter){ Pet(filter: $filter) { name } }`,
  };

  if (searchTerm.value) {
    body.variables = {
      filter: {
        name: { like: searchTerm.value },
      },
    };
  }

  const response = await $fetch("/pet store/graphql", {
    method: "POST",
    body: body,
  });

  listboxData.value = response.data.Pet.map((entry: Pet) => entry.name).sort();
}

watch(searchTerm, async () => {
  getExampleData();
});

onBeforeMount(() => {
  getExampleData();
});
</script>

<template>
  <h2 class="text-heading-2xl">Listbox component</h2>
  <div
    class="flex flex-row flex-wrap md:flex-nowrap justify-start items-start gap-4"
  >
    <div class="sm:w-[65%]">
      <h3 class="text-heading-lg mb-2"></h3>
      <InputLabel
        id="listbox-input-label"
        for="listbox-input"
        class="block mb-3 pl-0"
        :disabled="listboxState === 'disabled'"
        :has-error="listboxState === 'error'"
      >
        <span>Select a pet from the pet store</span>
      </InputLabel>
      <InputListbox
        id="listbox-input"
        labelId="listbox-input-label"
        :options="listboxData"
        :invalid="listboxState === 'error'"
        :valid="listboxState === 'valid'"
        :disabled="listboxState === 'disabled'"
        @update:model-value="(value: any) => (modelValue = value)"
        @search="(value: string) => searchTerm = value"
      />
    </div>
    <form class="[&>div]:py-2 [&>div>label]:block">
      <legend class="mb-2 text-heading-lg">Settings</legend>
      <div class="p-2">
        <InputLabel for="listbox-component-state" class="pl-0">
          Change the component state
        </InputLabel>
        <InputRadioGroup
          id="listbox-component-state"
          v-model="listboxState"
          :options="[
            {
              value: 'disabled',
              label: 'Disabled',
            },
            {
              value: 'error',
              label: 'Error',
            },
            {
              value: 'valid',
              label: 'Valid',
            },
          ]"
          :showClearButton="true"
        />
      </div>
    </form>
  </div>
  <div>
    <h3 class="text-heading-lg mb-2">Component output</h3>
    <output class="block w-full mt-2 border py-3 px-2 pl-6">
      <code>Search Term: {{ searchTerm }}</code>
    </output>
    <output class="block w-full mt-6 border py-3 px-2 pl-6">
      <code> User selection: {{ modelValue }} </code>
    </output>
  </div>
</template>
