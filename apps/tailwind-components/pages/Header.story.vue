<template>
  <div>
    <Header>
      <template #logo>
        <Logo />
      </template>
      <template #nav>
        <Navigation
          :navigation="[
            { label: 'Home', link: '#' },
            { label: 'About', link: '#' },
            { label: 'Contact', link: '#' },
          ]"
        />
      </template>
      <template #logo-mobile>
        <LogoMobile />
      </template>
      <template #nav-mobile>
        <Navigation
          :navigation="[
            { label: 'Home', link: '#' },
            { label: 'About', link: '#' },
            { label: 'Contact', link: '#' },
          ]"
        />
      </template>
    </Header>
  </div>
</template>
