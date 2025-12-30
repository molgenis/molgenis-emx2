<template>
  <div @click.stop>
    <input
      type="checkbox"
      :id="checkboxIdentifier"
      class="add-to-cart"
      @change.prevent="handleCollectionStatus"
      :checked="isChecked"
      :value="false"
      hidden
    />
    <label
      v-if="!iconOnly"
      class="add-to-cart-label btn btn-outline-secondary px-2"
      :for="checkboxIdentifier"
    >
      <span v-if="!multi">{{ uiText["add"] }}</span>
      <span v-else>{{ uiText["add_all"] }}</span>
    </label>
    <label v-else class="add-to-cart-label btn" :for="checkboxIdentifier">
      <span class="fa-regular fa-square fa-lg" :style="checkboxFaStyle"></span>
    </label>
    <label
      v-if="!iconOnly"
      class="btn remove-from-cart-label btn-outline-danger px-2"
      :for="checkboxIdentifier"
    >
      <span v-if="!multi">{{ uiText["remove"] }}</span>
      <span v-else>{{ uiText["remove_all"] }}</span>
    </label>
    <label v-else class="btn remove-from-cart-label" :for="checkboxIdentifier">
      <span
        class="fa-regular fa-check-square fa-lg"
        :style="checkboxFaStyle"
      ></span>
    </label>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";
import { IBiobanks, ICollections } from "../../interfaces/directory";

const checkoutStore = useCheckoutStore();
const settingsStore = useSettingsStore();

const props = withDefaults(
  defineProps<{
    biobankData: IBiobanks;
    collectionData: ICollections | ICollections[];
    iconOnly?: boolean;
    bookmark?: boolean;
    checkboxFaStyle?: any;
    multi?: boolean;
  }>(),
  {
    iconOnly: false,
    bookmark: false,
    checkboxFaStyle: { color: "var(--secondary)" },
    multi: false,
  }
);

const uiText = computed(() => settingsStore.uiText);

const checkboxIdentifier = computed(() =>
  Array.isArray(props.collectionData)
    ? `selector-${Math.random().toString().substring(2)}`
    : props.collectionData.id
);

const initialData = computed(() =>
  Array.isArray(props.collectionData)
    ? props.collectionData
    : [props.collectionData]
);

const collections = computed(() =>
  initialData.value.map((collection) => ({
    label: collection.name,
    value: collection.id,
  }))
);

function handleCollectionStatus(event: any) {
  const { checked } = event.target;
  if (checked) {
    checkoutStore.addCollectionsToSelection(
      props.biobankData,
      collections.value.map((collection) => ({
        label: collection.label,
        value: collection.value,
      })),
      props.bookmark
    );
  } else {
    checkoutStore.removeCollectionsFromSelection(
      { name: props.biobankData.name },
      collections.value.map((collection) => collection.value),
      props.bookmark
    );
  }
}

const isChecked = computed(() => {
  const selectedCollections =
    checkoutStore.selectedCollections[props.biobankData.name];

  if (selectedCollections) {
    const selectedCollectionIds = selectedCollections.map((sc) => sc.value);

    return collections.value
      .map((collection) => collection.value)
      .every((id) => selectedCollectionIds.includes(id));
  }
  return false;
});
</script>

<style scoped>
.btn {
  padding: 0 0.34rem;
}

.btn:hover {
  cursor: pointer;
}

.add-to-cart:checked ~ .add-to-cart-label {
  display: none;
}

.remove-from-cart-label {
  display: none;
}

.add-to-cart:checked ~ .remove-from-cart-label {
  display: inline-block;
}

.remove-from-cart-label:hover {
  cursor: pointer;
  opacity: 0.8;
}
</style>
