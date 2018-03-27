# Altitude #

## Build & Run ##

```sh
$ cd Altitude
$ ./sbt
> container:start
```

## Running tests against a particular database:

    sbt> testOnly software.altitude.test.core.suites.[SqliteSuite|PostgresSuite]

## Running a tagged test(s):
    sbt> testOnly -- -n focused
    sbt> testOnly software.altitude.test.core.suites.SqliteSuite -- -n focused
