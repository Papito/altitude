
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
