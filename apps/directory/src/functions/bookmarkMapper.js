import router from "../router";
import { useFiltersStore } from '../stores/filtersStore'
import { useCollectionStore } from '../stores/collectionStore';
// import { useRoute, useRouter } from 'vue-router';
import { useCheckoutStore } from '../stores/checkoutStore';

function setBookmark (bookmark) {
    router.push(
        {
            name: router.currentRoute.name,
            query: bookmark
        },
    )
}

export async function applyBookmark (watchedQuery) {

    let query = watchedQuery;

    if (!query) {
        const route = router.currentRoute.value

        if (!route.query) return

        query = route.query
    }


    /** reset the cart and the filters */
    if (!query) return

    const collectionStore = useCollectionStore();
    const filtersStore = useFiltersStore();

    /** do not apply when bookmark is changed from the app itself. */
    if (filtersStore.filterTriggeredBookmark) {
        filtersStore.filterTriggeredBookmark = false
        return
    }

    /** we load the filters, grab the names, so we can loop over it to map the selections */
    const filters = Object.keys(filtersStore.facetDetails)
  
    // if (query.nToken) {
    //   state.nToken = query.nToken
    // }

    // if (query.satisfyAll) {
    //   Vue.set(state.filters, 'satisfyAll', decodeURIComponent(query.satisfyAll).split(','))
    // }

    if (query.cart) {
        const checkoutStore = useCheckoutStore();

        const decoded = decodeURIComponent(query.cart)
        const cartIdString = window.atob(decoded)
        const cartIds = cartIdString.split(',')

        const missingCollections = await collectionStore.getMissingCollectionInformation(cartIds)

        for (const collection of missingCollections) {
            checkoutStore.addCollectionsToSelection({ biobank: collection.biobank, collections: [{ label: collection.name, value: collection.id }] })
        }

        /** add the beginning of history if from a link-back url */
        // if (state.searchHistory.length === 0) {
        //     state.searchHistory.push('Starting with a preselected list of collections')
        // }
    }

    for (const filterName of filters) {
        if (query[filterName]) {
            const filterOptions = filtersStore.filterOptionsCache[filterName];
            let queryValues = decodeURIComponent(query[filterName]).split(',')
            const filtersToAdd = filterOptions.filter(fo => queryValues.includes(fo.value))

            filtersStore.updateFilter(filterName, filtersToAdd)
        }
    }
}
export function createBookmark (filters, collectionCart) {
    const filtersStore = useFiltersStore();
    const bookmark = {}
    const matchAll = []

    if (filters) {
        const activeFilters = Object.keys(filters);
        for (const filterName of activeFilters) {

            const value = filters[filterName]

            /** can't do if(!value) because that would also trigger if value === 0 */
            if (value === '' || value === null || value === undefined || value.length === 0) { continue }

            const filterType = filtersStore.getFilterType(filterName)

            if (filterType === 'all') {
                matchAll.push(filterName)
            }

            if (Array.isArray(value) && value.length > 0) {
                const extractedValues = value.map(value => value['value'])
                bookmark[filterName] = encodeURI(extractedValues.join(','))
            } else if (typeof value === 'object') {
                bookmark[filterName] = encodeURI(value['value'])
            } else {
                bookmark[filterName] = encodeURI(value)
            }
        }
    }

    /** This manages the selection in the cart */
    if (collectionCart && Object.keys(collectionCart).length) {

        const bookmarkIds = []
        for (const biobank in collectionCart) {
            bookmarkIds.push(collectionCart[biobank].map(s => s.value));
        }

        const encodedCart = window.btoa(bookmarkIds.join(',')).toString('base64')
        bookmark.cart = encodeURI(encodedCart)
    }

    if (matchAll.length) {
        bookmark.matchAll = encodeURI(matchAll.join(','))
    }
    if (Object.keys(bookmark).length) {
        setBookmark(bookmark)
    }
}

export default {
    createBookmark
}
