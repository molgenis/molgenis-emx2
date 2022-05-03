<template>
  <div style="display: flex; align-self: flex-end">
    <label>
      <slot />
    </label>
  </div>
</template>

<script>
/** Layout multiple icons as a toolbar. */
export default {};
</script>

<docs>
    Example
    ```
    <template>
        <IconBar>
            <IconDanger icon="trash" @click="alert('clicked trash')"/>
            <IconAction icon="edit" @click="alert('clicked edit')"/>
        </IconBar>
    </template>
    <script>
        export default {
            methods: {
                alert(text) {
                    alert(text)
                }
            }
        }
    </script>
    ```
</docs>
