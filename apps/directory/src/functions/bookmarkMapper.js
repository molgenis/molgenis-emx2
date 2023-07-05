import router from '../router'

function setBookmark (bookmark) {
    console.log(bookmark)

    router.push(
        {
            name: router.currentRoute.name,
            query: bookmark
        },
    )
}

export const createBookmark = (filters, collectionCart) => {


    console.log(filters, collectionCart)
    // const { selections, satisfyAll } = filters

    const bookmark = {}
    // /** Selections is an object which holds the information on every filter about which option / string has been supplied */
    // if (selections && Object.keys(selections).length > 0) {
    //     for (const property in selections) {
    //         const value = selections[property]

    //         /** can't do if(!value) because that would also trigger if value === 0 */
    //         if (value === '' || value === null || value === undefined || value.length === 0) { continue }

    //         if (Array.isArray(value) && value.length > 0) {
    //             bookmark[property] = encodeURI(value.join(','))
    //         } else {
    //             bookmark[property] = encodeURI(value)
    //         }
    //     }
    // }

    /** This manages the selection in the cart */
    if (collectionCart && Object.keys(collectionCart).length) {
        const bookmarkIds = []
        for(const biobank in collectionCart) {
             bookmarkIds.push(collectionCart[biobank].map(s => s.value));
        }
       
        const encodedCart = window.btoa(bookmarkIds.join(',')).toString('base64')
        bookmark.cart = encodeURI(encodedCart)
    }

    // if (satisfyAll && satisfyAll.length) {
    //     bookmark.satisfyAll = encodeURI(satisfyAll.join(','))
    // }

    setBookmark(bookmark)
}

export default {
    createBookmark
}
