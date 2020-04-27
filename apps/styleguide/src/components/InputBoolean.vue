<template>
    <InputRadio v-model="value" :defaultValue="defaultValue" :items="[true, false]" v-bind="$props"/>
</template>

<script>
    import _baseInput from './_baseInput'
    import InputRadio from './InputRadio'

    /** Show true/false switch */
    export default {
        components: {InputRadio},
        extends: _baseInput
    }
</script>

<docs>
    Example
    ```
    <InputBoolean label="My first boolean" help="do you need some boolean help?"/>
    ```
    Example with defaultValue
    ```
    <InputBoolean :defaultValue="false" label="My first boolean" help="do you need some boolean help?"/>
    ```
</docs>
