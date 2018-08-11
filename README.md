# http-server 

This is a sample http server, very simplified to only service GET requests to static files found in this repository.

## Run it.

Assuming you have a maven client installed, it should be very simple:

1. mvn test<CR>

This will run the sequence of maven targets up to and including test.  Items should be installed, code check-styled, compiled and finally
test cases that start the http-server up and execute simplified fetches against it.

OR,

mvn mvn exec:java<CR>

This will do the same, winding up with the http-server staying up so that you can make browser requests to it locally, such as:

Then you can hit: http:localhost:8081/helloworld.html
Then you can hit: http:localhost:8081/ERROR_chrome_2018-08-01T20-43-00.756Z.png
Then you can hit: http:localhost:8081/foo/introducing_cairngorm.pdf

ctrl<C> to terminate.

## Methodology

My approach...

## Shortcuts

Code I lifted...
