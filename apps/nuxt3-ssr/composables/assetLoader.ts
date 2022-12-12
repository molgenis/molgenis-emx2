import tailwindConfig from '#tailwind-config'
export default {
    load: async function (assetName: string) {
        const logos = import.meta.glob('../assets/logos/**/*.svg', { as: 'raw' });
        const match = logos[`../assets/logos/${assetName}.svg`]
        return match()
    },
    loadThemeConfig: function () {
        return tailwindConfig?.theme
    }
}