var Util = {
  getGridAdjustment: function(containerEl, boxSize, boxBorder) {
    var containerWidth = containerEl.width();
    var containerHeight = containerEl.height();

    var viewportW = containerWidth - (containerWidth * 0.02);

    var boxPadding = parseInt(boxSize * 0.05);
    var boxMargin = parseInt(boxSize * 0.05);

    var boxWidth = boxSize + (boxMargin + boxPadding + boxBorder) * 2;

    var fitsHorizontally = parseInt(viewportW / boxWidth, 10);
    var viewPortRemainder = viewportW - (fitsHorizontally * boxWidth);
    var remainderPerSide = parseInt(viewPortRemainder / (fitsHorizontally * 2));

    var boxHeight = (boxSize + boxPadding + boxBorder) * 2;
    var boxSideMargin = boxMargin +remainderPerSide;

    return {
      fitsHorizontally: fitsHorizontally,
      boxWidth: boxWidth,
      boxHeight: boxHeight,
      boxMargin: boxMargin,
      boxPadding: boxPadding,
      boxSideMargin: boxSideMargin,
      remainderPerSide: remainderPerSide,
      containerHeight: containerHeight,
      containerWidth: containerWidth
    }
}};
