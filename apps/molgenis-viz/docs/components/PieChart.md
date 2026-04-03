# PieChart

Create a pie chart to visually display subelements of your data in relation to the entire dataset. The data should contain no more than 7 elements and all group-values pairs that are less than 1% must be combined into an "other" category. Colors should be used to highlight interesting findings rather than emphasizing groups. However, if you require a group-based color scheme, make sure colors are accessible and use a *muted* color palette. Please note that group differences can be emphasized by enabling animations. 

## Props

<!-- @vuese:PieChart:props:start -->
|Name|Description|Type|Required|Default|
|---|---|---|---|---|
|chartId|A unique ID for the chart|`String`|`true`|-|
|title|A title that describes the chart|`String`|`false`|-|
|description|Additional information to display below the title|`String`|`false`|-|
|chartData|An object containing 7 or fewer group-value pairs|`Object`|`true`|-|
|valuesAreShown|If true (default), values will be rendered with the labels|`Boolean`|`false`|true|
|valuesArePercents|If true, labels will be generated with a percent sign.|`Boolean`|`false`|true|
|chartHeight|set the height of the chart. Width is determined by the dimensions of the parent container so that the chart is responsive. If you would like to specify the width of the chart, use CSS or adjusted the `chartHeight`.|`Number`|`false`|`300`|
|chartMargins|set all chart margins|`Number`|`false`|`20`|
|chartScale|set chart scale|`Number`|`false`|`0.75`|
|chartColors|An object containing one-to-one mappings of groups to colors. If colors are not defined, a default palette will be chosen for you.|`Object`|`false`|null|
|strokeColor|Set the border color for the slices|`String`|`false`|`#3f454b`|
|asDonutChart|If `true`, the pie chart will be rendered as a donut chart (i.e., with the center cut out).|`Boolean`|`false`|false|
|centerAlignChart|If true, the chart will be aligned to the center of the parent component. Otherwise, the chart will be left aligned.|`Boolean`|`false`|false|
|enableHoverEvents|If `true`, hover events will be enabled for all slices. A hover event will highlight the hovered slice by increasing the font size of the labels and increasing the size of the slice. The slice will return the default state when it is no longer hovered.|`Boolean`|`false`|`true`|
|enableClicks|If `true`, click events will be enabled for all slices. When a slice is clicked, the row-level data for that bar will be emitted. To access the data, use the event `@slice-clicked=...`|`Boolean`|`false`|`false`|

<!-- @vuese:PieChart:props:end -->


## Events

<!-- @vuese:PieChart:events:start -->
|Event Name|Description|Parameters|
|---|---|---|
|slice-clicked|-|-|

<!-- @vuese:PieChart:events:end -->


