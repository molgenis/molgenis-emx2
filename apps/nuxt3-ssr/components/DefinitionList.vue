<script setup lang="ts">
const { small, items } = withDefaults(
  defineProps<{
    items: any[];
    small?: boolean;
  }>(),
  {
    small: false,
  }
);

const isArray = (value: []) => {
  return Array.isArray(value);
};

const useGridClasses = "grid md:grid-cols-3 md:gap-2.5";
const smallClasses = "";
</script>

<template>
  <dl class="grid gap-2.5 text-body-base text-gray-900">
    <div :class="small ? smallClasses : useGridClasses" v-for="item in items" :key="item.label">
      <dt class="flex items-start font-bold text-body-base">
        <div class="flex items-center gap-1">
          {{ item.label }}
          <div v-if="item.tooltip">
            <CustomTooltip label="Lees meer" :content="item.tooltip" />
          </div>
        </div>
      </dt>

      <dd class="col-span-2" :class="{ 'mb-2.5': small }">
        <ContentOntology v-if="item?.type === 'ONTOLOGY'" :tree="item.content" :collapse-all="false"></ContentOntology>
        <ul v-else-if="isArray(item.content)" class="grid gap-1 pl-4 list-disc list-outside">
          <li v-for="row in item.content" :key="row">
            {{ row }}
          </li>
        </ul>
        <p v-else>
          {{ item.content }}
        </p>
      </dd>
    </div>
  </dl>
</template>
