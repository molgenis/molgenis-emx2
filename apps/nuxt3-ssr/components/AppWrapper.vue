<template>
  <div
    class="min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden">
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient">
      <BackgroundGradient />
    </div>
    <div class="relative z-30">
      <slot name="header"></slot>
      <main>
        <slot></slot>
      </main>
      <slot name="footer"></slot>
    </div>
  </div>
</template>
