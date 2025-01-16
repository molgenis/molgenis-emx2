<template>
  <div class="flex flex-col gap-4">
    <h2 class="text-2xl text-title">Large</h2>
    <div class="flex gap-4">
      <Button type="primary" size="large">Primary</Button>
      <Button type="secondary" size="large">Secondary</Button>
      <Button type="tertiary" size="large">tertiary</Button>
      <Button type="outline" size="large">outline</Button>
      <Button type="disabled" size="large">disabled</Button>
      <Button type="filterWell" size="large">filterWell</Button>
    </div>

    <h2 class="text-2xl text-title">Medium</h2>
    <div class="flex gap-4">
      <Button type="primary" size="medium">Primary</Button>
      <Button type="secondary" size="medium">Secondary</Button>
      <Button type="tertiary" size="medium">tertiary</Button>
      <Button type="outline" size="medium">outline</Button>
      <Button type="disabled" size="medium">disabled</Button>
      <Button type="filterWell" size="medium">filterWell</Button>
    </div>

    <h2 class="text-2xl text-title">Small</h2>
    <div class="flex gap-4">
      <Button type="primary" size="small">Primary</Button>
      <Button type="secondary" size="small">Secondary</Button>
      <Button type="tertiary" size="small">tertiary</Button>
      <Button type="outline" size="small">outline</Button>
      <Button type="disabled" size="small">disabled</Button>
      <Button type="filterWell" size="small">filterWell</Button>
    </div>

    <h2 class="text-2xl text-title">Tiny</h2>
    <div class="flex gap-4">
      <Button type="primary" size="tiny">Primary</Button>
      <Button type="secondary" size="tiny">Secondary</Button>
      <Button type="tertiary" size="tiny">tertiary</Button>
      <Button type="outline" size="tiny">outline</Button>
      <Button type="disabled" size="tiny">disabled</Button>
      <Button type="filterWell" size="tiny">filterWell</Button>
    </div>

    <h2 class="text-2xl text-title">Icons</h2>
    <div class="flex gap-4">
      <Button type="primary" size="medium" icon="filter" iconPosition="left"
        >Filter</Button
      >
      <Button type="primary" size="small" icon="filter" iconPosition="right"
        >Filter</Button
      >
      <Button type="secondary" size="small" icon="star" iconPosition="left"
        >Star</Button
      >
      <Button
        type="secondary"
        size="small"
        icon="CaretDown"
        iconPosition="right"
        >CaretDown</Button
      >
    </div>
  </div>
</template>
