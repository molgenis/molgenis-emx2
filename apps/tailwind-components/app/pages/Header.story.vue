<template>
  <div>
    <Header>
      <template #logo>
        <Logo />
      </template>
      <template #nav>
        <Navigation
          :navigation="[
            { label: 'Home', location: '#' },
            { label: 'About', location: '#' },
            { label: 'Contact', location: '#' },
          ]"
        />
      </template>
      <template #logo-mobile>
        <LogoMobile />
      </template>
      <template #nav-mobile>
        <Navigation
          :navigation="[
            { label: 'Home', location: '#' },
            { label: 'About', location: '#' },
            { label: 'Contact', location: '#' },
          ]"
        />
      </template>
    </Header>
  </div>
</template>
