<template>
  <div
    class="overflow-x-clip min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden"
  >
    <div
      class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient"
    >
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative min-h-screen flex flex-col">
      <main class="mb-auto">
        <Header class="border-b border-divider bg-navigation">
          <template #logo><Logo link="/" /> </template>
          <template #nav>
            <Navigation
              :navigation="[
                { label: 'Tables', link: '#' },
                { label: 'Graphql', link: '#' },
                { label: 'Help', link: '#' },
                { label: 'About', link: '#' },
              ]"
            />
          </template>
          <template #account></template>
        </Header>
        <slot></slot>
      </main>

      <FooterComponent />
    </div>
  </div>
</template>
