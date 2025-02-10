function getOffsetTop(element: HTMLElement): number {
    let offsetTop = 0;
    while (element) {
        offsetTop += element.offsetTop;
        element = element.offsetParent as HTMLElement;
    }
    return offsetTop;
}


export function scrollToElementInside(containerId: string, elementInsideId: string): void {
    const scrollableContainerElement = document.getElementById(containerId);
    const elementInside = document.getElementById(elementInsideId);

    if (!scrollableContainerElement) {
        console.error(`Scrollable container with ID "${containerId}" not found.`);
        return;
    }

    if (!elementInside) {
        console.error(`Element inside with ID "${elementInsideId}" not found.`);
        return;
    }

    const containerOffsetTop = getOffsetTop(scrollableContainerElement); // Get container's full offset
    const elementInsideOffsetTop = getOffsetTop(elementInside); // Get element's full offset

    const elementOffsetWithinContainer = elementInsideOffsetTop - containerOffsetTop;

    const elementHeight = elementInside.offsetHeight;
    const containerHeight = scrollableContainerElement.offsetHeight;

    const topIsBeyondThreshold = elementHeight < containerHeight && elementOffsetWithinContainer > containerHeight / 2;

    // Add padding if we're near the end
    if (topIsBeyondThreshold) {
        const padding = containerHeight - elementHeight;
        scrollableContainerElement.style.paddingBottom = padding + "px"
    } else {
        scrollableContainerElement.style.paddingBottom = "0px"
    }

    scrollableContainerElement.scrollTop = elementOffsetWithinContainer;
}