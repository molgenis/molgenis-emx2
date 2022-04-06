<template>
  <div class="input-group">

    <div class="input-group-prepend">      
        <slot name="prepend"></slot>
    </div>

    <slot></slot>

    <div class="input-group-append">
      <slot name="append">
        
      </slot>
    </div>
  </div>
</template>

