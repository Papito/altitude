# Altitude #

## Build & Run ##

```sh
$ sbt
> watch
```

`watch` starts Jetty server while watching files for changes. 

## Running a tagged test(s):
Update your test as such:

```
test("work in progress", Focused) {
}
```
    sbt> testFocused
    sbt> tetestFocusedSqlite
    sbt> tetestFocusedPostgres

## Running tests against a particular database:

    sbt> testOnly software.altitude.test.core.suites.[SqliteSuite|PostgresSuite]

## Remote debugging

1. Create a regular `Remote` run configuration (port 5005)
2. Start Jetty server via SBT as usual
3. Run the configuration created in Step 1

TEST
