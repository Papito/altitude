
export const state = {
    snackbar: {
        type: "info",
        message: "",
        show: false,
    }
}

export function highlightNav(elClass) {
    const fullClassName = `nav .menu.${elClass}`
    document.querySelector(fullClassName).classList.add("active");
}

export function showModal({minWidthPx, title}) {
    htmx.find("#modalTitle").innerText = title

    if (minWidthPx) {
        htmx.find("#modalContent").style.width = `${minWidthPx}px`
    }

    htmx.find("#modalContainer").style.display = "block"
}

export function closeModal() {
    htmx.find("#modalContainer").style.display = "none"
}

export function clearInnerNodes(node) {
    while (node.hasChildNodes()) {
        _clearNode(node.firstChild);
    }
}

function _clearNode(node) {
    while (node.hasChildNodes()) {
        _clearNode(node.firstChild);
    }
    node.parentNode.removeChild(node);
}

export function closeFolderContextMenu(menuEl) {
    clearInnerNodes(menuEl)
    menuEl.innerHTML = ""
    menuEl.style.display = "none"
}

export function clearFolderChildNodes(folderId) {
    const childEls = document.querySelectorAll("#children-" + folderId + " .folder")
    childEls.forEach(child => {
        clearInnerNodes(child)
        child.innerHTML = ""
        child.style.display = "none"
    })
}

/**
 * Default action of the ESC key is to close the modal
 */
document.onkeydown = function(evt) {
    evt = evt || window.event
    let isEscape
    if ("key" in evt) {
        isEscape = (evt.key === "Escape" || evt.key === "Esc")
    } else {
        isEscape = (evt.keyCode === 27)
    }
    if (isEscape) {
        closeModal()
    }
}
