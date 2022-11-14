<script setup lang="ts">

const config = useRuntimeConfig()

const { data, pending, error, refresh } = await useFetch(
    "/catalogue/catalogue/graphql",
    {
        baseURL: config.public.apiBase,
        method: "POST",
        body: {
            query: '{Cohorts {name, pid}}'
        }
    }
);

async function log() {
    const { data, pending, error, refresh } = await useFetch(
        "/catalogue/catalogue/graphql",
        {
            baseURL: config.public.apiBase,
            method: "POST",
            body: {
                query: '{Cohorts {name, pid}}'
            }
        }
    );
}

</script>

<template>
    <div>
        <h1 class="text-center text-5xl">
            European Networks Health Data & Cohort Catalogue. test
        </h1>
        <button @click="log">Click mij</button>
        {{ data }}

    </div>
</template>