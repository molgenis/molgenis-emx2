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

<script>
import { useCheckoutStore } from "../../stores/checkoutStore";
import { useSettingsStore } from "../../stores/settingsStore";

export default {
  setup() {
    const checkoutStore = useCheckoutStore();
    const settingsStore = useSettingsStore();

    return { checkoutStore, settingsStore };
  },
  props: {
    biobankData: {
      type: Object,
      required: true,
    },
    collectionData: {
      type: [Object, Array],
      required: true,
    },
    iconOnly: {
      type: Boolean,
      required: false,
      default: false,
    },
    bookmark: {
      type: Boolean,
      required: false,
      default: false,
    },
    checkboxFaStyle: {
      type: Object,
      required: false,
      default: function () {
        return {
          color: "var(--secondary)",
        };
      },
    },
    multi: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  data: () => {
    return {
      collections: [],
      identifier: "",
    };
  },
  methods: {
    handleCollectionStatus(event) {
      const { checked } = event.target;
      const collectionData = {
        biobank: this.biobankData,
        collections: JSON.parse(JSON.stringify(this.collections)),
        bookmark: this.bookmark,
      };

      if (checked) {
        this.checkoutStore.addCollectionsToSelection(collectionData);
      } else {
        this.checkoutStore.removeCollectionsFromSelection(collectionData);
      }
    },
  },
  computed: {
    checkboxIdentifier() {
      return this.identifier;
    },
    isChecked() {
      const biobankIdentifier = this.biobankData.label || this.biobankData.name;
      const selectedCollections = this.checkoutStore.selectedCollections[
        biobankIdentifier
      ];

      if (selectedCollections) {
        const selectedCollectionIds = selectedCollections.map((sc) => sc.value);

        return this.collections
          .map((collection) => collection.value)
          .every((id) => selectedCollectionIds.includes(id));
      }
      return false;
    },
    uiText() {
      return this.settingsStore.uiText;
    },
  },
  mounted() {
    let initialData;

    if (Array.isArray(this.collectionData)) {
      initialData = this.collectionData;
      this.identifier = `selector-${Math.random().toString().substring(2)}`;
    } else {
      initialData = [this.collectionData];
      this.identifier = this.collectionData.id;
    }

    this.collections = initialData.map((collection) => ({
      label: collection.label || collection.name,
      value: collection.id,
    }));
  },
};
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
