<template>
    <ul v-if="networks">
        <li v-for="d in networks" :key="d.acronym">
            <RouterLink
                :to="{
                    name: 'network',
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
  props: { networks: Array },
};
</script>
