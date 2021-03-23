<template>
    <ul v-if="partners && partners.length > 0">
        <li v-for="p in partners" :key="p.acronym">
            <RouterLink
                :to="{
                    name: 'institution',
                    params: { acronym: p.institution.acronym },
                }"
            >
                {{ p.institution.acronym }} - {{ p.institution.name }}
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
    partners: Array,
  },
};
</script>
