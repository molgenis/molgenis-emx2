# Pie Charts

Pie charts are commonly used to show _parts of a whole_ where each segment (or slice) represents are percentage of a category. It is expected that sum of the values is always equal to 100%.

## Tips for success

1. It is recommended to keep the number of categories to a minimum. Ideally, no more than 5.
2. Values that are less than 10% should be combined into an "other" category.
3. Always use a clear title and description to provide context for users

## Properties

An astrix (`*`) indicates a required property.

### ID\*

A string that identifies a single pie chart.

```vue
<PieChart id="participant-recruitment-by-group" .../>
```

### Title\*

A title for the chart.

```vue
<PieChart title="Allocation of participants by group" ... />
```

### Description

An optional description that provides additional context for a chart.

```vue
<PieChart description="Participants (n=100) were assigned to Group A, B or C" .../>
```

### Height and Width

A number that determines the dimensions of the chart. By default, the height and width is `300`. However, the chart also responds to the size of the parent element, so it is recommended to set width outside of this component.

```vue
<PieChart :width="300" :height="300" ... />
```

### Margins

A number that sets the vertical and horizontal spacing around the pie chart. This is useful if you need to add extra space for labels. By default, the margin is set to `25`. Increasing the margins will make the chart smaller as there is less space to generate chart. As a result, you may need to adjust the height or width.

```vue
<PieChart margins="50" .../>
```

### Data

The data used to generate the pie chart.

Data must be an object that contains one or more key-value pairs. Make sure the sum of is 100% and values less than 10% should be combined into a single group (e.g., "Other").

```ts
const data = { A: 12, B: 82 };
```

```vue
<PieChart :data="data" .../>
```

### Color Palette

A custom color palette that will override the default palette (Blues). The palette must be an object of key-value pairs. Keys must exist in the chart dataset. Any valid CSS color value can be used; e.g., names (`red`, `cornflowerblue`), hex (`#bdbdbd`), etc.

```ts
const data = { A: 12, B: 82 };
const colors = { A: "red", B: "blue" };
```

```vue
<PieChart :data="data" :color-palette="colors" .../>
```
