<template>
  <div style="display: flex;   align-self: flex-end;">
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
    <IconBar>
        <IconDanger icon="trash
    "/>
        <IconAction icon="edit"/>
    </IconBar>
    ```
</docs>
