function Asset(data) {
    this.id = data ? data.id : null;
    this.path = data? data.path: null;
    this.fileName = data ? data.fileName : null;
    this.createdAt = data ? data.createdAt : null;
    this.updatedAt = data ? data.updatedAt : null;
}