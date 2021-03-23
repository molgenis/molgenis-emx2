<template>
    <ul v-if="datasources">
        <li v-for="d in datasources" :key="d.acronym">
            <RouterLink
                :to="{
                    name: 'datasource',
                    params: { acronym: d.acronym },
                }"
            >
                {{ d.acronym }} - {{ d.name }}
            </RouterLink>
        </li>
    </ul>
    <p v-else>
        N/A
    </p>
</template>

<script>
export default {
  props: {
    datasources: Array,
  },
};
</script>
