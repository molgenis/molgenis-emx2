<template>
  <ButtonBar>
    <Button icon="caret-up" size="small">one</Button>
  </ButtonBar>

  <ButtonBar class="mt-4">
    <Button icon="caret-up" size="small">one </Button>
    <Button icon="caret-down" size="small">two</Button>
  </ButtonBar>

  <ButtonBar class="mt-4">
    <Button icon="caret-up" size="small">one </Button>
    <Button icon="info" size="small">two </Button>
    <Button icon="caret-down" size="small">three</Button>
    <Button icon="trash" size="small">four</Button>
  </ButtonBar>

  <ButtonBar class="mt-4">
    <Button :icon-only="true" icon="caret-up" label="up up up !" />
    <Button :icon-only="true" icon="caret-down" label="going down" />
  </ButtonBar>
</template>
