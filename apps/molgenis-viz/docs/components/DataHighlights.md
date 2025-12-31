# DataHighlights

Data highlights are used to display an interesting value for a given scenario (e.g., total of *x*, recruit to date, etc.). This component is designed to give a quick look &mdash;or highlight of&mdash; variables of interest. It is suggested to use this component at the top of a dashboard and to display 3 to 5 values. It is not recommended to display more than 4 or one value. Titles must be short and consise as this element can be rather small. Limit to one or two words max. 

## Props

<!-- @vuese:DataHighlights:props:start -->
|Name|Description|Type|Required|Default|
|---|---|---|---|---|
|title|one or two words that describes the value|`String`|`false`|-|
|showTitle|If false, titles will be visually hidden|`Boolean`|`false`|`false`|
|data|An object containing one or more key-value pairs|`Object`|`true`|-|

<!-- @vuese:DataHighlights:props:end -->


