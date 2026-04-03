<template>
  <div
    class="collection-spotlight d-flex flex-column"
    :style="css.collectionSpotlight.backgroundStyle"
    ref="collectionSpotlight"
  >
    <h1 class="ml-4 mt-4 header-text">
      <span>{{ headerText }}</span>
    </h1>
    <section class="ml-4 mb-4">
      <template v-for="(collection, index) in collections">
        <div :key="collection.id + '-' + index" v-if="collection.name">
          <h3
            class="border-top mr-4 pt-4 mb-2 mt-1 collection-header"
            :title="collection.name"
          >
            {{ truncateTitle(collection.name) }}
          </h3>
          <router-link
            :to="'/collection/' + collection.id"
            :title="`Go to ${collection.name}`"
          >
            <span
              :class="css.collectionSpotlight.linkClasses"
              :style="css.collectionSpotlight.linkStyle"
              >{{ collection.linkText }}</span
            >
          </router-link>
        </div>
      </template>
    </section>
  </div>
</template>

<script>
export default {
  props: {
    css: {
      type: Object,
      required: false,
      default: () => ({
        collectionSpotlight: {
          backgroundStyle: "background-color: var(--info);",
          linkClasses: "text-info",
          linkStyle: "",
        },
      }),
    },
    headerText: {
      type: String,
      required: true,
    },
    collections: {
      type: Array,
      required: true,
    },
  },
  methods: {
    truncateTitle(title) {
      if (title.length > 70) {
        return title.substring(0, 70) + "...";
      }

      return title;
    },
  },
  mounted() {
    /** retrieving the hexcode from the css property */
    const hexCode = getComputedStyle(
      this.$refs.collectionSpotlight
    ).getPropertyValue("--info");
    /** adding an opacity */
    this.$refs.collectionSpotlight.style.backgroundColor = `${hexCode}16`;
  },
};
</script>

<style scoped>
.collection-spotlight {
  width: 45%;
  border-radius: 1rem;
}

.collection-header {
  font-size: 1.2rem;
  word-break: break-all;
}
</style>
