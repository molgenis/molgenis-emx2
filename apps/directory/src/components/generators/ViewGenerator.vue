<template>
  <div>
    <table class="layout-table w-100">
      <tbody>
        <component
          v-for="attribute in attributes"
          :is="component(attribute.type)"
          :attribute="attribute"
          :key="attribute.id"
        />
      </tbody>
    </table>

    <component
      v-for="customComponent in customComponents"
      :is="customComponent.component"
      :attribute="customComponent"
      :key="customComponent.id"
    />

    <div
      v-if="
        renderObject.sub_collections && renderObject.sub_collections.length > 0
      "
      class="mt-3"
    >
      <h3>Subcollections</h3>

      <subcollection
        v-for="sub_collection of renderObject.sub_collections"
        :key="sub_collection.id"
        :collection="sub_collection"
      />
    </div>
  </div>
</template>

<script lang="ts">
import mref from "./view-components/mref.vue";
import array from "./view-components/array.vue";
import string from "./view-components/string.vue";
import longtext from "./view-components/longtext.vue";
import quality from "./view-components/quality.vue";
import hyperlink from "./view-components/hyperlink.vue";
import Subcollection from "./view-components/Subcollection.vue";

export default {
  name: "ViewGenerator",
  components: {
    mref,
    longtext,
    quality,
    array,
    string,
    hyperlink,
    Subcollection,
  },
  props: {
    viewmodel: {
      type: Object,
      required: true,
    },
  },
  computed: {
    renderObject() {
      return this.viewmodel;
    },
    attributes() {
      return this.renderObject.attributes.filter(
        (attr: Record<string, any>) => !attr.component
      );
    },
    customComponents() {
      return this.renderObject.attributes.filter(
        (attr: Record<string, any>) => attr.component
      );
    },
  },
  methods: {
    component(type: string) {
      switch (type) {
        case "categoricalmref": {
          return "mref";
        }
        case "longtext":
        case "quality":
        case "array":
        case "mref":
        case "hyperlink": {
          return type;
        }
        default: {
          return "string";
        }
      }
    },
  },
};
</script>

<style>
.layout-table {
  border-collapse: unset;
}
</style>
