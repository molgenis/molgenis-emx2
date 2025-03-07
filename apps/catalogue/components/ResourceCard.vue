<script setup lang="ts">
import dateUtils from "~/utils/dateUtils";
import type { IResources } from "~/interfaces/catalogue";

const datasetStore = useDatasetStore();

const cutoff = 250;

const route = useRoute();

const props = withDefaults(
  defineProps<{
    resource: IResources;
    schema: string;
    compact?: boolean;
    catalogue?: string;
  }>(),
  {
    compact: false,
  }
);

const startEndYear = dateUtils.startEndYear;
const isInShoppingCart = ref<boolean>(false);

const articleClasses = computed(() => {
  return props.compact ? "py-5 lg:px-12.5 p-5" : "lg:px-12.5 py-12.5 px-5";
});

const subtitleClasses = computed(() => {
  return props.compact ? "hidden md:block" : "mt-1.5 block md:inline";
});

const titleContainerClasses = computed(() => {
  return props.compact ? "flex items-center" : "";
});

const headerClasses = computed(() => {
  return props.compact ? "" : "items-start xl:items-center";
});

function onInput() {
  console.log(props.resource);
  isInShoppingCart.value = datasetStore.resourceIsInCart(props.resource.id);
  if (isInShoppingCart.value) {
    datasetStore.removeFromCart(props.resource.id);
  } else {
    datasetStore.addToCart(props.resource);
  }
}

watch([datasetStore.datasets], () => {
  isInShoppingCart.value = datasetStore.resourceIsInCart(props.resource.id);
});
</script>

<template>
  <article :class="articleClasses">
    <header :class="headerClasses" class="flex">
      <div :class="titleContainerClasses" class="grow">
        <h2 class="min-w-[160px] mr-4 md:inline-block block">
          <NuxtLink
            :to="`/${schema}/catalogue/${catalogue}/${route.params.resourceType}/${resource.id}`"
            class="text-body-base font-extrabold text-blue-500 hover:underline hover:bg-blue-50"
          >
            {{ resource?.acronym || resource?.name }}
          </NuxtLink>
        </h2>

        <span :class="subtitleClasses" class="mr-4 text-body-base">
          {{ resource?.acronym ? resource?.name : "" }}
        </span>
      </div>
      <div class="flex">
        <label
          :for="`${resource.id}-shopping-cart-input`"
          class="xl:flex xl:justify-end px-2 py-1 rounded-3px cursor-pointer text-blue-500 hover:text-blue-800 focus:text-blue-800"
          :class="{
            'items-baseline xl:items-center mt-0.5 xl:mt-0': !compact,
            'bg-blue-500 text-white hover:text-white': isInShoppingCart,
          }"
        >
          <BaseIcon name="shopping-cart-add" :width="21" />
          <span class="sr-only"></span>
          <input
            type="checkbox"
            :id="`${resource.id}-shopping-cart-input`"
            class="sr-only"
            v-model="isInShoppingCart"
            @input="onInput"
          />
        </label>
        <NuxtLink
          :to="`/${schema}/catalogue/${catalogue}/resources/${resource.id}`"
        >
          <IconButton
            icon="arrow-right"
            class="text-blue-500 hidden xl:flex xl:justify-end"
          />
        </NuxtLink>
      </div>
    </header>

    <div v-if="!compact">
      <ContentReadMore :text="resource.description" :cutoff="cutoff" />

      <dl class="hidden xl:flex gap-5 xl:gap-14 text-body-base">
        <div>
          <dt class="flex-auto block text-gray-600">Type</dt>
          <dd>{{ resource?.type?.map((type) => type.name).join(",") }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Design</dt>
          <dd>{{ resource?.design?.name }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Participants</dt>
          <dd>{{ resource?.numberOfParticipants }}</dd>
        </div>
        <div>
          <dt class="flex-auto block text-gray-600">Duration</dt>
          <dd>
            {{
              startEndYear(
                resource?.startYear?.toString(),
                resource?.endYear?.toString()
              )
            }}
          </dd>
        </div>
      </dl>
    </div>
  </article>
</template>
