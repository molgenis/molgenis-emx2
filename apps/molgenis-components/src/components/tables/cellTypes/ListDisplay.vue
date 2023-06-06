<template>
  <div>
    <button
      v-show="data.length > foldCuttOff"
      @click.stop.prevent="onFoldClicked"
      class="btn btn-sm float-right fold-btn"
    >
      <template>
        <svg
          v-if="isFolded"
          aria-hidden="true"
          focusable="false"
          data-prefix="fas"
          data-icon="chevron-down"
          class="svg-inline--fa fa-chevron-down fa-w-14"
          role="img"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 448 512"
        >
          <path
            fill="currentColor"
            d="M207.029 381.476L12.686 187.132c-9.373-9.373-9.373-24.569 0-33.941l22.667-22.667c9.357-9.357 24.522-9.375 33.901-.04L224 284.505l154.745-154.021c9.379-9.335 24.544-9.317 33.901.04l22.667 22.667c9.373 9.373 9.373 24.569 0 33.941L240.971 381.476c-9.373 9.372-24.569 9.372-33.942 0z"
          ></path>
        </svg>
        <svg
          v-else
          aria-hidden="true"
          focusable="false"
          data-prefix="fas"
          data-icon="chevron-up"
          class="svg-inline--fa fa-chevron-up fa-w-14"
          role="img"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 448 512"
        >
          <path
            fill="currentColor"
            d="M240.971 130.524l194.343 194.343c9.373 9.373 9.373 24.569 0 33.941l-22.667 22.667c-9.357 9.357-24.522 9.375-33.901.04L224 227.495 69.255 381.516c-9.379 9.335-24.544 9.317-33.901-.04l-22.667-22.667c-9.373-9.373-9.373-24.569 0-33.941L207.03 130.525c9.372-9.373 24.568-9.373 33.941-.001z"
          ></path>
        </svg>
      </template>
    </button>
    <div v-for="(listItem, index) in visibleListItems" :key="index">
      <component
        :is="cellTypeComponentName"
        :data="listItem"
        :metaData="metaData"
      ></component>
    </div>
  </div>
</template>

<script>
import ObjectDisplay from "./ObjectDisplay.vue";
import StringDisplay from "./StringDisplay.vue";
import EmailDisplay from "./EmailDisplay.vue";
import HyperlinkDisplay from "./HyperlinkDisplay.vue";

const typeMap = {
  REF_ARRAY: "ObjectDisplay",
  ONTOLOGY_ARRAY: "ObjectDisplay",
  REFBACK: "ObjectDisplay",
  EMAIL_ARRAY: "EmailDisplay",
  HYPERLINK_ARRAY: "HyperlinkDisplay",
};

export default {
  name: "ListDisplay",
  components: { ObjectDisplay, StringDisplay, EmailDisplay, HyperlinkDisplay },
  data() {
    return {
      isFolded: true,
    };
  },
  props: {
    data: {
      type: [Array],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
    foldCuttOff: {
      type: Number,
      required: false,
      default: () => 3,
    },
  },
  computed: {
    cellTypeComponentName() {
      return typeMap[this.metaData.columnType] || "StringDisplay";
    },
    visibleListItems() {
      return this.isFolded ? this.data.slice(0, this.foldCuttOff) : this.data;
    },
  },
  methods: {
    onFoldClicked(event) {
      this.isFolded = !this.isFolded;
      // remove click focus (without destoying tab focus)
      event.target.blur();
      event.target.parentElement.blur();
    },
  },
};
</script>

<style scoped>
.fold-btn {
  margin-top: -1rem;
  width: 3rem;
  height: 3rem;
  text-align: right;
}

.fa-chevron-up {
  width: 0.66rem;
  height: 0.66rem;
}

.fa-chevron-down {
  width: 0.66rem;
  height: 0.66rem;
}
</style>
