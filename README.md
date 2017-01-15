# Altitude #

## Build & Run ##

```sh
$ cd Altitude
$ ./sbt
> container:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Running tests against a particular database:

    testOnly integration.[SqliteSuite|PostgresSuite]
