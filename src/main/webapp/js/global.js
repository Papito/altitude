state = {
    snackbarTimeout: null,
}

function closeModal() {
    htmx.find("#modalContainer").style.display = "none"
}

function showModal({minWidthPx, title}) {
    htmx.find("#modalTitle").innerText = title

    if (minWidthPx) {
        htmx.find("#modalContent").style.width = `${minWidthPx}px`
    }

    htmx.find("#modalContainer").style.display = "block"
}

function _showSnackbar({type, message}) {
    let elSnackbar = htmx.find("#snackbar");

    elSnackbar.classList.remove("success")
    elSnackbar.classList.remove("warning")

    elSnackbar.innerHTML = message
    elSnackbar.classList.add("show")

    if (type === "success") {
        elSnackbar.classList.add("success")
    } else {
        elSnackbar.classList.add("warning")
    }

    if (state.snackbarTimeout) {
        clearTimeout(state.snackbarTimeout)
    }

    state.snackbarTimeout = setTimeout(function() {
        elSnackbar.classList.remove("show")
        elSnackbar.innerHTML = ""
    }, 3000);
}

function showSuccessSnackBar (message) {
    _showSnackbar({type: "success", message: message})
}

function showWarningSnackBar (message) {
    _showSnackbar({type: "warning", message: message})
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

function highlightNav(elClass) {
    const fullClassName = `nav .menu.${elClass}`
    document.querySelector(fullClassName).classList.add("active");
}
