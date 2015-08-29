package altitude.exceptions

class MetadataExtractorException(ex: Throwable)
  extends Exception("Metadata extractor system error", ex)
