<template>
  <header class="antialiased px-5 lg:px-0 xl:bg-navigation">
    <Container>
      <div class="items-center justify-between hidden xl:flex h-25">
        <slot name="logo"></slot>
        <slot name="nav"></slot>
        <div class="w-[450px]"></div>

        <slot name="account">
          <HeaderButton label="Account" icon="user" />
        </slot>
      </div>

      <div class="pt-5 xl:hidden">
        <div class="relative flex items-center h-12.5 justify-between mb-4">
          <div class="absolute -translate-x-1/2 left-1/2">
            <slot name="logo-mobile"></slot>
          </div>
          <div class="flex gap-3">
            <HeaderButton label="Account" icon="user" />
          </div>
        </div>

        <slot name="nav-mobile"></slot>
        <div class="w-full pt-6"></div>
      </div>
    </Container>
  </header>
</template>
