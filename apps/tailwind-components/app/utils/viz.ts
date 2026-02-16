
export function set_chart_legend_layout_css(legendIsEnabled: boolean, legendPosition: string): string {
    if (legendIsEnabled && legendPosition) {
        return `chart_layout_with_legend_${legendPosition}`;
    }
    return `chart_layout_default`;
}