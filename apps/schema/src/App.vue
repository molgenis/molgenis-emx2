<template>
    <div id="app">
        <Molgenis :menuItems="menuItems" :title="title" id="__top">
            <router-view/>
        </Molgenis>
    </div>
</template>

<script>
    import {
        Molgenis
    } from '@mswertz/emx2-styleguide'

    export default {
        components: {
            Molgenis
        },
        data: function () {
            return {
                schema: null
            }
        },
        computed: {
            title() {
                return this.$route.params.schema + ': ' + this.$route.name;
            },
            menuItems() {
                return [
                    {label: 'Home', href: '#/' + this.$route.params.schema},
                    {label: 'Design', href: '#/' + this.$route.params.schema + "/design/"},
                    {label: 'Members', href: '#/' + this.$route.params.schema + "/members/"}

                ]
            }
        }
    }
</script>
