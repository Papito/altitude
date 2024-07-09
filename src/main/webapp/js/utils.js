function highlightNav(elClass) {
    const fullClassName = `nav .menu.${elClass}`
    document.querySelector(fullClassName).classList.add("active");
}

function showModal({minWidthPx, title}) {
    htmx.find("#modalTitle").innerText = title

    if (minWidthPx) {
        htmx.find("#modalContent").style.width = `${minWidthPx}px`
    }

    htmx.find("#modalContainer").style.display = "block"
}

function closeModal() {
    htmx.find("#modalContainer").style.display = "none"
}

function clearInnerNodes(node) {
    while (node.hasChildNodes()) {
        clearNode(node.firstChild);
    }
}

function clearNode(node) {
    while (node.hasChildNodes()) {
        clearNode(node.firstChild);
    }
    node.parentNode.removeChild(node);
}

function closeFolderContextMenu(menuEl) {
    clearInnerNodes(menuEl)
    menuEl.innerHTML = ""
    menuEl.style.display = "none"
}

function clearFolderChildNodes(folderId) {
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
