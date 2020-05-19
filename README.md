# Altitude #

## Build & Run ##

```sh
$ sbt
> jettyr:start
```

## Running tests against a particular database:

    sbt> testOnly software.altitude.test.core.suites.[SqliteSuite|PostgresSuite]

## Running a tagged test(s):
    sbt> testFocused
    sbt> tetestFocusedSqlite
    sbt> tetestFocusedPostgres
    
## Remote debugging

1. Create a regular `Remote` run configuration (port 5005)
2. Start Jetty server via SBT as usual
3. Run the configuration created in Step 1
