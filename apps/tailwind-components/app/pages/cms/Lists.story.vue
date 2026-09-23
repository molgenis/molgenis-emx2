<script setup lang="ts">
import OrderedList from "../../components/cms/lists/OrderedList.vue";
import UnorderedList from "../../components/cms/lists/UnorderedList.vue";

const items = [
  "This is the first item in the list",
  "This is the second line that's really long, which is useful for longer blocks of text. By default, these text elements wrap to a new line.",
  "This line has HTML: <code>value=1234</code>, <strong>this text is bold</strong>, and <em>this is italicised</em>.",
  "And this is the last",
];
</script>

<template>
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-7.5 text-title-contrast">
    <div class="p-2.5">
      <p class="font-bold mb-2.5">Ordered List</p>
      <OrderedList id="ordered-list-demo" :orderedItems="items" />
    </div>
    <div class="p-2.5">
      <p class="font-bold mb-2.5">Unordered List</p>
      <UnorderedList id="unordered-list-demo" :unorderedItems="items" />
    </div>
  </div>
</template>
