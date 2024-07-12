export const Const = {
    events: {
        folderMoved: "FOLDER_MOVED_EVENT",
        folderDeleted: "FOLDER_DELETED_EVENT",
        folderAdded: "FOLDER_ADDED_EVENT",
        folderStateChanged: "FOLDER_STATE_CHANGED_EVENT",
        folderCollapsed: "FOLDER_COLLAPSED_EVENT",
    },

    attributes: {
        expanded: "expanded",
        // CAREFUL! This is still hard-coded, once, in the root folder element
        isRoot: "is-root",
        numOfChildren: "num-of-children",
    }
}
