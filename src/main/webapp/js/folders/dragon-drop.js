/**
 * Folder drag'n'drop functionality
 * ================================
 */
import {Const} from "/js/constants.js";
import interact from "https://cdn.interactjs.io/v1.9.20/interactjs/index.js"

/**
 * Maintain position of the dragged element in order to snap it back at cancel
 * https://github.com/taye/interact.js/issues/819#issuecomment-626599750
 */
const dragState = {
    x: 0,
    y: 0,
}

function dragged(e) {
    dragState.x = 0;
    dragState.y = 0;
    e.target.style.transform = 'translate(0px, 0px)';

    // reset the position attributes for draggables (on failed drag, it will resume from the last position)
    e.target.removeAttribute("data-x")
    e.target.removeAttribute("data-y")
}

interact('#rootFolderList .draggable')
    .draggable({
        // enable inertial throwing
        inertia: true,
        // keep the element within the area of it's parent
        modifiers: [
            interact.modifiers.restrictRect({
                restriction: '#rootFolderList > div.children',
                endOnly: true,
                elementRect: {
                    top: 0,
                    left: 0,
                    bottom: 1,
                    right: 1
                }
            })
        ],
        // enable autoScroll
        autoScroll: true,

        listeners: {
            move: dragMoveListener,
            end: dragged,
        }
    })

function dragMoveListener (event) {
    dragState.x += event.dx;
    dragState.y += event.dy;

    event.target.style.transform = 'translate(' + dragState.x + 'px, ' + dragState.y + 'px)';

    const target = event.target
    // keep the dragged position in the data-x/data-y attributes
    const x = (parseFloat(target.getAttribute('data-x')) || 0) + event.dx
    const y = (parseFloat(target.getAttribute('data-y')) || 0) + event.dy

    // translate the element
    target.style.transform = 'translate(' + x + 'px, ' + y + 'px)'

    // update the position attributes
    target.setAttribute('data-x', x)
    target.setAttribute('data-y', y)
}

// enable a draggable to be dropped into this
interact('#rootFolderList .dropzone').dropzone({
    // only accept elements matching this CSS selector
    accept: '#rootFolderList .draggable',
    // going above seems to break the dropzone
    overlap: 0.50,

    // listen for drop related events:

    ondropactivate: function (event) {
        // add active dropzone feedback
        event.target.classList.add('drop-active')
    },
    ondragenter: function (event) {
        const draggableElement = event.relatedTarget
        const dropzoneElement = event.target

        // feedback the possibility of a drop
        dropzoneElement.classList.add('drop-target')
        draggableElement.classList.add('can-drop')
    },
    ondragleave: function (event) {
        // remove the drop feedback style
        event.target.classList.remove('drop-target')
        event.relatedTarget.classList.remove('can-drop')
    },
    ondrop: function (event) {
        const draggableElement = event.relatedTarget
        const dropzoneElement = event.target

        dropzoneElement.classList.remove('drop-active')
        dropzoneElement.classList.remove('drop-target')

        draggableElement.classList.remove('can-drop')

        const movedFolderId = draggableElement.getAttribute("folder-id")
        const newParentFolderId = dropzoneElement.getAttribute("folder-id")

        const movedFolderEvent = new CustomEvent(
            Const.events.folderMoved, {
                detail: {
                    movedFolderId: movedFolderId,
                    newParentId: newParentFolderId,
    }});

        document.body.dispatchEvent(movedFolderEvent)
    },

    ondropdeactivate: function (event) {
        // remove active dropzone feedback
        event.target.classList.remove('drop-active')
        event.target.classList.remove('drop-target')
    }
})

interact('#rootFolderList .drag-drop')
    .draggable({
        inertia: true,
        modifiers: [
            interact.modifiers.restrictRect({
                restriction: '#rootFolderList .dropzone',
                endOnly: true
            })
        ],
        autoScroll: true,
        // dragMoveListener from the dragging demo above
        listeners: { move: dragMoveListener }
    })
