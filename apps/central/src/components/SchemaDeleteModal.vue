<template>
    <div>
        <!-- whilst loading -->
        <LayoutModal
                v-if="loading"
                :title="title"
                :show="true">
            <template v-slot:body>
                <Spinner/>
            </template>
        </LayoutModal>
        <!-- when completed -->
        <LayoutModal
                v-else-if="success"
                :title="title"
                :show="true"
                @close="$emit('close')">
            <template v-slot:body>
                <MessageSuccess>{{ success }}</MessageSuccess>
            </template>
            <template v-slot:footer>
                <ButtonAction @click="$emit('close')">Close</ButtonAction>
            </template>
        </LayoutModal>
        <!-- create schema -->
        <LayoutModal v-else :title="title" :show="true" @close="$emit('close')">
            <template v-slot:body>
                <MessageError v-if="error">{{ error }}</MessageError>
                Are you sure you want to delete schema '{{schemaName}}'
            </template>
            <template v-slot:footer>
                <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
                <ButtonAction @click="executeDeleteSchema">Delete schema
                </ButtonAction>
            </template>
        </LayoutModal>
    </div>
</template>

<script>
    import {request} from 'graphql-request'

    import {
        MessageSuccess,
        MessageError,
        ButtonAction,
        ButtonAlt,
        LayoutModal,
        LayoutForm,
        Spinner,
        IconAction
    } from '@mswertz/emx2-styleguide'

    export default {
        components: {
            MessageSuccess,
            MessageError,
            ButtonAction,
            ButtonAlt,
            LayoutModal,
            LayoutForm,
            Spinner,
            IconAction
        },
        props: {
            schemaName: String
        },
        data: function () {
            return {
                key: 0,
                loading: false,
                error: null,
                success: null,
            }
        },
        computed: {
            title() {
                return "Delete group"
            },
            endpoint() {
                return '/api/graphql'
            }
        },
        methods: {
            executeDeleteSchema() {
                this.loading = true
                this.error = null
                this.success = null
                request(
                    this.endpoint,
                    `mutation deleteSchema($name:String){deleteSchema(name:$name){message}}`,
                    {
                        name: this.schemaName
                    }
                )
                    .then(data => {
                        this.success = data.deleteSchema.message;
                        this.loading = false
                    })
                    .catch(error => {
                        if (error.response.status === 403) {
                            this.error = error.message + 'Forbidden. Do you need to login?'
                        } else {
                            this.error = error.response.errors[0].message
                        }
                        this.loading = false
                    })
            }
        }
    }
</script>
