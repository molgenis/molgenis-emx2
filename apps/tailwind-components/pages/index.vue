<script setup lang="ts">
const modules = import.meta.glob('./*.story.vue',{
  import: 'default',
  eager: true,
} )

const stories = []
const components = []
Object.entries(modules).forEach(([key, value]) => {
    console.log(key)
    console.log(value)
    stories.push(key)
    components.push(value)
})

</script>
<template>
    <ul>
        <li v-for="story in stories">{{ story }}</li>

    </ul>
    <div v-for="component in components">
        <div>
            <component :is="component" />
        </div>
    </div>
</template>