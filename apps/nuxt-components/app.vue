
<template>
  <section class="max-w-lg mx-auto lg:px-5 px-0">
    <slot name="header"></slot>
    <div class="xl:flex">
      <aside class="xl:min-w-95 xl:w-95 hidden xl:block border-r-2">

        <h2 class="text-2xl font-bold my-5">Components</h2>

        <ul class="list-none">
          <li class="py-2">
            <NuxtLink to="/">Home</NuxtLink>
          </li>
          <li class="py-2">
            <NuxtLink to="Comp1.story">Comp1</NuxtLink>
          </li>
          <li class="py-2">
            <NuxtLink to="CustomTooltip.story">CustomTooltip</NuxtLink>
          </li>
        </ul>

        <div class="pr-6 mt-6">
          <hr>
        </div>
        <div class="mt-3">
          
          <a c href="/_tailwind/" target="_blank">Styles </a>
        </div>

      

      </aside>
      <div class="xl:pl-7.5 xl:max-w-[54rem] 2xl:grow 2xl:max-w-none">
        <div class="mt-3">
          <NuxtPage />
        </div>

      </div>
    </div>
  </section>
</template>

