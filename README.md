# Altitude #

## Build & Run ##

```sh
$ cd Altitude
$ ./sbt
> container:start
> browse
```

If `browse` does not launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Running tests against a particular database:

    sbt> test-only test-only software.altitude.test.core.suites.[SqliteSuite|PostgresSuite]

## Running a tagged test(s):
    sbt> test-only -- -n Current
    sbt> test-only test-only software.altitude.test.core.suites.SqliteSuite -- -n Current
