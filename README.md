# Forex Proxy API

> [Forex](https://github.com/paidy/interview/blob/master/forex-mtl) is a simple application that acts as a local proxy for getting exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies
> Create a `live` interpreter for the `oneframe` service. This should consume the [one-frame API](https://hub.docker.com/r/paidyinc/one-frame).

## Assumptions and limitations

1. Rates are not invertible, i.e. AUD-SGD != 1 / SGD-AUD
2. Limit for calling external service is 1000 requests per day
3. It is possible to get an unlimited number of rates from external service via one request
4. Rate returned by the proxy shouldn't be older than 5 minutes
5. Proxy should be able to serve at least 10000 requests per day (one every ≈8 seconds)
6. It is possible that external service could fail or time out

## Implementation

Given these assumptions, I built an application that uses caching to reduce the number of calls to the external service.
With the default configuration (can be changed) the application works like this:

- `OneFrameLive` service has a maximum allowed timeout of 30 seconds
- `RateStoreLive` service requests rates for all possible, pre-defined pairs of currencies using `OneFrame` service every 2 minutes.
- `OneFrameLiveRates` service will return the cached pair using `RatesBoard` service and will check if the rate is not older than 5 minutes, otherwise will return an error.

This setup allows us to have pretty fresh data for rates: ≈2 minutes old if underlying service works correctly and in normal conditions, it makes 720 requests per day to the external service.

So this setup allows the clients of the Proxy have fresh data while staying beyond the limit of requests to the external service.

All the errors are wraped into the `ErrorResponse(message: String)` model and returned as JSON body with suitable HTTP status code.

## Build and run
There are multiple ways to run the Proxy.

### docker-compose
The repository provides the `Dockerfile` for the project, it relies on the fat-jar file, it also provides `docker-compose.yml` file for running both the Proxy and the one-frame container.
In order to run the project using this method, do the following:

- Execute `sbt clean assembly` in the shell, this will output the fat-jar file at `./target/scala-2.13/forex-assembly-1.0.1.jar`
- Execute `docker-compose up --build`, this will build the image for the Proxy and pull the OneFrame image. After this, it will start two containers in the same network: `one_frame` and `forex_proxy_api`. API will be exposed on the **port 8000**.

### Dockerfile
If you have an external OneFrame service you can just build the image of the Proxy and run it.
In order to run the project using this method, do the following:

- Execute `sbt clean assembly` in the shell, this will output the fat-jar file at `./target/scala-2.13/forex-assembly-1.0.1.jar`
- Execute `docker build -t forex_proxy_api ./`. This will build an image called `forex_proxy_api`
- Execute `docker run -e ONE_FRAME_HOST_NAME={host} -e ONE_FRAME_PORT={port} forex_proxy_api`. Instead of `{host}` and `{port}` provide valid OneFrame service host and port

## Run test
- Execute `sbt test`

### Accessing API

Now you can query the rates on `http://127.0.0.1:9000/rates`, for example:
```shell
curl 'http://127.0.0.1:8000/rates?from=USD&to=CAD'
```

## Possible improvements
- More tests, integration tests
- Better logging, metrics
- Add retry mechanism for one_frame
