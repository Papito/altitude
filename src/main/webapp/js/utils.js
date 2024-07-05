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
