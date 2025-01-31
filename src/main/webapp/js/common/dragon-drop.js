import { Const } from "../constants.js"

const dragState = {
    x: 0,
    y: 0,
}

export function dragged(event) {
    dragState.x = 0
    dragState.y = 0

    let target = event.target
    target.style.position = "relative"
    target.style.top = "auto"
    event.target.style.transform = "translate(0px, 0px)"

    // reset the position attributes for draggables (on failed drag, it will resume from the last position)
    event.target.removeAttribute("data-x")
    event.target.removeAttribute("data-y")

    let imgElement = target.querySelector("img")
    if (imgElement) {
        imgElement.style.width =
            imgElement.getAttribute(Const.attributes.originalWidth) + "px"
        imgElement.removeAttribute(Const.attributes.originalWidth)
    }
}

export function dragMoveListener(event) {
    dragState.x += event.dx
    dragState.y += event.dy

    event.target.style.transform =
        "translate(" + dragState.x + "px, " + dragState.y + "px)"

    const target = event.target
    // keep the dragged position in the data-x/data-y attributes
    const x = (parseFloat(target.getAttribute("data-x")) || 0) + event.dx
    const y = (parseFloat(target.getAttribute("data-y")) || 0) + event.dy

    // translate the element
    target.style.transform = "translate(" + x + "px, " + y + "px)"
    // update the position attributes
    target.setAttribute("data-x", x)
    target.setAttribute("data-y", y)
}

// Setting element as "fixed" will let us drag it outside the parent container boundaries (between panels)
export function setFixedPositionWhileDragging(event) {
    console.debug("Setting fixed position while dragging")
    let target = event.target
    let position = target.getBoundingClientRect()

    target.style.position = "fixed"
    target.style.top = position.top + "px"
}
