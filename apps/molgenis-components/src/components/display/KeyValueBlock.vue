<template>
  <grid-block :heading="computedHeading">
    <read-more
      v-if="items.length === 1 && items[0].value"
      :text="items[0].value"
      :length="750"
    />
    <div v-else-if="items.length > 1">
      <div v-for="(item, index) in items" :key="index">
        <strong>{{ item.label }}</strong>
        <template v-if="Array.isArray(item.value)">
          <ul class="list-unstyled">
            <li
              v-for="(valueItem, valueItemIndex) in item.value"
              :key="valueItemIndex"
            >
              {{ valueItem }}
            </li>
          </ul>
        </template>
        <template v-else>
          <p>{{ item.value }}</p>
        </template>
      </div>
    </div>
  </grid-block>
</template>

<script>
import ReadMore from "../layout/ReadMore.vue";
import GridBlock from "./GridBlock.vue";

export default {
  name: "KeyValueBlock",
  components: { GridBlock, ReadMore },
  props: {
    heading: {
      type: String,
      required: false,
    },
    /**
     * List of items
     * Item has:
     * key: String (required)
     * label: String (required)
     * value: String, Number, Boolean, Object
     */
    items: {
      type: Array,
      required: false,
      default: () => [],
    },
  },
  computed: {
    computedHeading() {
      return this.heading
        ? this.heading
        : this.items.length === 1
        ? this.items[0].label
        : null;
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label class="font-italic">
      single item example ( label is hidden or used as heading )
    </label>
    <key-value-block
        heading="my heading"
        :items="[{ key: 'key1', label: 'label 1', value: 'value 1' }]"
    ></key-value-block>

    <label class="font-italic">multiple items example</label>
    <key-value-block
        heading="my heading"
        :items="[
        { key: 'key1', label: 'label 1', value: 'value 1' },
        { key: 'key2', label: 'label 2', value: 'value 2' },
      ]"
    ></key-value-block>
  </div>
</template>
</docs>
