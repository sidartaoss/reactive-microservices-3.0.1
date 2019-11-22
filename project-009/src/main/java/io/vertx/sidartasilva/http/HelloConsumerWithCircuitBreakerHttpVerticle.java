package io.vertx.sidartasilva.http;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import rx.Single;


public class HelloConsumerWithCircuitBreakerHttpVerticle extends AbstractVerticle {

    private WebClient hello;
    private CircuitBreaker circuit;

    private boolean ready;

    @Override
    public void start() {
        circuit = CircuitBreaker.create("my-circuit", vertx,
                new CircuitBreakerOptions()
                .setFallbackOnFailure(true)     // Call the fallback on failures
                .setTimeout(2000)               // Set the operation timeout
                .setMaxFailures(3)              // Number of failures before 
                                                // switching to the 'OPEN'state
                .setResetTimeout(5000)
        );

        Router router = Router.router(vertx);
        router.get("/").handler(this::invokeHelloMicroservice);

        router.get("/health").handler(HealthCheckHandler.create(vertx)
                .register("http-server-consumer-running", future -> future.complete(
                    ready ? Status.OK() : Status.KO())
        ));

        // Create the service discovery instance
        ServiceDiscovery.create(vertx, discovery -> {
            // Look for a Http endpoint named as project008
            Single<WebClient> single = HttpEndpoint.rxGetWebClient(
                discovery, rec -> rec.getName().equalsIgnoreCase("project008"));

            // In the half-open state, we rediscover the endpoint
            circuit.halfOpenHandler(v -> single.subscribe(
                client -> this.hello = client
            ));

            single.subscribe(
                client -> {
                    // the configured hello to call our microservice
                    this.hello = client;

                    // start the Http server
                    vertx.createHttpServer()
                            .requestHandler(router::accept)
                            .listen(8081, ar -> ready = ar.succeeded());

                },
                err -> System.out.println("Oh no, no service")
            );

        });

    }

    private void invokeHelloMicroservice(RoutingContext rc) {
        circuit.rxExecuteCommandWithFallback(
            future -> {
                HttpRequest<JsonObject> request1 = hello.get("/Adam")
                    .as(BodyCodec.jsonObject());
                
                HttpRequest<JsonObject> request2 = hello.get("/Eve")
                    .as(BodyCodec.jsonObject());

                Single<JsonObject> s1 = request1.rxSend().map(HttpResponse::body);

                Single<JsonObject> s2 = request2.rxSend().map(HttpResponse::body);

                Single.zip(s1, s2, (adam, eve) -> {
                    // We have the result of both request in Adam and Eve
                    return new JsonObject()
                            .put("adam", adam.getString("message") + " " + adam.getString("served-by"))
                            .put("eve", eve.getString("message") + " " + eve.getString("served-by"));
                    
                })
                .subscribe(future::complete, future::fail);
            },
            error -> new JsonObject().put("message", "hello (fallback, " + circuit.state().toString() + ")")
        )   
        .subscribe(
            x -> rc.response().end(x.encodePrettily()),
            t -> rc.response().end(t.getMessage())
        );
    }

}
