<template>
  <ul v-if="contacts">
    <li v-for="c in contacts">
      {{ c.name }}
      <span v-if="c.email">
        (<a :href="'mailto:' + c.email">{{ c.email }} </a>)
      </span>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
export default {
  props: {
    contacts: Array,
  },
};
</script>
