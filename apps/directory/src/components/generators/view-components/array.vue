<template>
  <tr v-if="attribute && attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">
      {{ displayName(attribute) }}
    </th>
    <td>
      <span>
        {{ value(attribute).join(", ") }}
      </span>
    </td>
  </tr>
</template>

<script>
export default {
  props: {
    attribute: {
      type: Object,
    },
  },
  methods: {
    displayName(item) {
      return item.label || item.name || item.id;
    },
    value(item) {
      /** sanity check  */
      if (!item || !item.value || !item.value.length) return [];

      const values = item.value;

      const isArray = Array.isArray(values);

      /** sanity check II */
      if (!isArray) return values;

      const isArrayOfObjects = typeof values[0] === "object";

      /** we have a just an array of values. Perfect to return */
      if (!isArrayOfObjects) return values;

      return values.map((value) => value.label || value.name || value.id);
    },
  },
};
</script>
