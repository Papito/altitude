import {Const} from "./constants.js";
import {clearInnerNodes} from "./utils.js";

export class Folder {
    constructor(id) {
        if (!id) {
            throw new Error("Folder id is required")
        }
        this.element = htmx.find("#folder-" + id)

        if (!this.element) {
            throw new Error("Folder element not found for id " + id)
        }
        this.id = id
        this.iconEl = htmx.find("#folder-icon-" + this.id)
        this.childrenEl = htmx.find("#children-" + this.id)
        this.isRoot = this.element.getAttribute(Const.attributes.isRoot) === "true"
    }

    static closeContextMenu(menuEl) {
        clearInnerNodes(menuEl)
        menuEl.innerHTML = ""
        menuEl.style.display = "none"
    }
    menuEl() {
        return htmx.find("#menu-" + this.id)
    }

    folderNameEl() {
        return htmx.find("#folderName-" + this.id)
    }

    closeContextMenu() {
        Folder.closeContextMenu(this.menuEl())
    }

    showContextMenu() {
        console.log("Showing context menu for folder " + this.name())
        this.menuEl().style.display = "flex"
    }

    clearChildren() {
        const childEls = this.childrenEl.querySelectorAll(".folder")
        childEls.forEach(child => {
            clearInnerNodes(child)
            child.innerHTML = ""
            child.style.display = "none"
        })
    }

    isMenuExpanded() {
        return this.menuEl().querySelector("span") !== null
    }

    isExpanded() {
        return this.element.getAttribute(Const.attributes.expanded) === "true"

    }

    name() {
        return this.folderNameEl().innerText
    }

    numOfChildren() {
        return parseInt(this.element.getAttribute(Const.attributes.numOfChildren))
    }

    incrementNumOfChildren() {
        console.log("Incrementing # of children for  " + this.name() + ". New value: " + this.numOfChildren())
        console.log("\tOld value: " + this.numOfChildren())
        this.element.setAttribute(Const.attributes.numOfChildren, this.numOfChildren() + 1)
        console.log("\tNew value: " + this.numOfChildren())
    }

    decrementNumOfChildren() {
        console.log("Decrementing # of children for  " + this.name() + ". New value: " + this.numOfChildren())
        console.log("\tOld value: " + this.numOfChildren())
        this.element.setAttribute(Const.attributes.numOfChildren, this.numOfChildren() - 1)
        console.log("\tNew value: " + this.numOfChildren())
    }

    updateVisualState() {
        console.log("Updating visual state for folder " + this.name())
        console.log("\tNumber of children: " + this.numOfChildren())

        if (this.isRoot) {
            console.log("\tRoot folder")
            return
        }

        if (!this.isExpanded()) {
            this.clearChildren()
            this.closeContextMenu()

            if (this.numOfChildren()) {
                console.log("\tFolder is collapsed and has children")
                this.iconEl.classList.remove("fa-folder-minus")
                this.iconEl.classList.add("fa-folder-plus")
            } else {
                console.log("\tFolder is collapsed and has no children")
                this.iconEl.classList.remove("fa-folder-plus")
                this.iconEl.classList.remove("fa-folder-minus")
                this.iconEl.classList.add("fa-folder")
            }
        }

        if (this.isExpanded()) {
            if (this.numOfChildren()) {
                console.log("\tFolder is expanded and has children")
                this.iconEl.classList.remove("fa-folder-plus")
                this.iconEl.classList.add("fa-folder-minus")
            } else {
                console.log("\tFolder is expanded and has no children")
                this.iconEl.classList.remove("fa-folder-plus")
                this.iconEl.classList.remove("fa-folder-minus")
                this.iconEl.classList.add("fa-folder")
            }
        }

    }

    parent() {
        const parentId= this.element.getAttribute(Const.attributes.parentFolderId)
        return new Folder(parentId)
    }

    collapse() {
        console.log("Setting folder " + this.name() + " to collapsed")
        this.element.removeAttribute(Const.attributes.expanded)
        this.updateVisualState()
    }

    expand() {
        console.log("Setting folder " + this.name() + " to expanded")
        this.element.setAttribute(Const.attributes.expanded, "true")
        this.updateVisualState()}

    remove() {
        this.element.remove()
    }

    addChild(folder) {
        // NOTE: increment/decrement of children must be done by the caller, NOT here
        this.childrenEl.appendChild(folder.element)
    }

    htmxExpandChildrenAction() {
        console.log("Triggering expand children action for " + this.name())
        const expandFolderChildrenEl = htmx.find("#" + "expand-folder-children-" + this.id)
        htmx.trigger(expandFolderChildrenEl, "click")

    }
}