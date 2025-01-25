import interact from "https://cdn.interactjs.io/v1.9.20/interactjs/index.js"
import { dragged, dragMoveListener } from "../common/dragon-drop.js"

interact(".person.drag-drop").draggable({
    listeners: {
        move: dragMoveListener,
        end: dragged,
        start: function (event) {
            let target = event.target
            let position = target.getBoundingClientRect()

            target.style.position = "fixed"
            target.style.top = position.top + "px"
        },
    },
})
