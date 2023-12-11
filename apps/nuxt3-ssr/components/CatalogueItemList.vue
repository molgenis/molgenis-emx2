<script setup lang="ts">
interface DefinitionListItem {
  label: string;
  tooltip?: string;
  type?: string;
  content: any;
}
withDefaults(
  defineProps<{
    items: DefinitionListItem[];
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

function emptyContent(item: DefinitionListItem) {
  if (item.content === undefined || item.content === "") {
    return false;
  } else if (Array.isArray(item.content) && item.content.length === 0) {
    return false;
  } else if (
    Object.keys(item.content).length === 0 &&
    Object.getPrototypeOf(item.content) === Object.prototype
  ) {
    // empty object
    return false;
  }

  return true;
}

function showAsFile(item: DefinitionListItem) {
  return item.content && item.content.url && item.content.extension;
}
</script>

<template>
  <dl class="grid gap-2.5 text-body-base text-gray-900">
    <div
      :class="small ? smallClasses : useGridClasses"
      v-for="item in items.filter(emptyContent)"
      :key="item.label"
    >
      <dt class="flex items-start font-bold text-body-base">
        <div class="flex items-center gap-1">
          {{ item.label }}
          <div v-if="item.tooltip">
            <CustomTooltip label="Read more" :content="item.tooltip" />
          </div>
        </div>
      </dt>

      <dd class="col-span-2" :class="{ 'mb-2.5': small }">
        <ContentOntology
          v-if="item?.type === 'ONTOLOGY'"
          :tree="buildOntologyTree(item.content)"
          :collapse-all="true"
        ></ContentOntology>

        <a v-else-if="showAsFile(item)" class="flex" :href="item.content.url">
          <div class="flex-start">
            <span class="text-blue-500 text-body-base">
              {{ item.label }}
            </span>
          </div>
        </a>

        <ul
          v-else-if="isArray(item.content) && item.content.length > 1"
          class="grid gap-1 pl-4 list-disc list-outside"
        >
          <li v-for="row in item.content" :key="row">
            {{ row }}
          </li>
        </ul>
        <p v-else-if="item?.content?.tooltip" class="flex items-center gap-1">
          {{ item.content.value }}
          <CustomTooltip label="Read more" :content="item.content.tooltip" />
        </p>
        <p v-else>
          {{ Array.isArray(item.content) ? item.content[0] : item.content }}
        </p>
      </dd>
    </div>
  </dl>
</template>
