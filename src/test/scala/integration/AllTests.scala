package integration

import org.scalatest.Suites
import unit.{SearchQueryModelTests, ModelTests, FolderModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new FileSystemImportTests(config),
  new MetadataParserTests(config),
  new FolderModelTests,
  new ModelTests,
  new SearchQueryModelTests,
  new ImportProfileTests(config),
  new SearchTests(config),
  new AssetServiceTests(config),
  new StatsServiceTests(config),
  new UserMetadataServiceTests(config),
  new FolderServiceTests(config),
  new LibraryServiceTests(config),
  new RepositoryTests(config)
)

