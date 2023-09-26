package com.lucaslamounier.reactiveprogramming;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

class FluxTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxTest.class);

    @Test
    void fluxSubscriber() {
        Flux<String> flux = Flux
                .just("a", "b", "c", "d")
                .log();

        StepVerifier
                .create(flux)
                .expectNext("a", "b", "c", "d")
                .verifyComplete();

    }

    @Test
    void fluxSubscriberNumbers() {
        Flux<Integer> flux = Flux
                .range(1, 5)
                .log();

        flux.subscribe((e) -> LOGGER.info("Number {}", e));

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();

    }

    @Test
    void fluxSubscriberFromIterable() {
        Flux<Integer> flux = Flux
                .fromIterable(List.of(1, 2, 3, 4, 5))
                .log();

        flux.subscribe((e) -> LOGGER.info("Number {}", e));

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();

    }

    @Test
    void fluxSubscriberNumbersError() {
        Flux<Integer> flux = Flux
                .range(1, 5)
                .log()
                .map(i -> {
                    if (i == 4) {
                        throw new IndexOutOfBoundsException("Error -> IndexOutOfBoundsException");
                    }
                    return i;
                });

        flux.subscribe(
                e -> LOGGER.info("Number {}", e),
                Throwable::printStackTrace,
                () -> LOGGER.info("Completed"));

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3)
                .expectError(IndexOutOfBoundsException.class)
                .verify();

    }

    @Test
    void fluxSubscriberNumbersWithBackPressure() {
        Flux<Integer> flux = Flux
                .range(1, 5)
                .log()
                .map(i -> {
                    if (i == 4) {
                        throw new IndexOutOfBoundsException("Error -> IndexOutOfBoundsException");
                    }
                    return i;
                });

        flux.subscribeWith(new BaseSubscriber<Integer>() {
            @Override
            protected void hookOnNext(Integer value) {
                LOGGER.info("Number {}", value);
            }


            @Override
            protected void hookOnError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            protected void hookOnComplete() {
                LOGGER.info("Completed");
            }

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(3);
            }
        });

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3)
                .expectError(IndexOutOfBoundsException.class)
                .verify();

    }

    @Test
    void fluxSubscriberNumbersWithBackPressureInPairsBaseSubscriber() {
        Flux<Integer> flux = Flux
                .range(1, 10)
                .log();

        flux.subscribe(new BaseSubscriber<>() {

            private int count = 0;
            private final int requestCount = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestCount);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count++;
                if (count >= requestCount) {
                    count = 0;
                    request(requestCount);
                }
            }
        });

        LOGGER.info(" ------------------------ ");

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

    }

    @Test
    void fluxSubscriberInterval() throws InterruptedException {
        Flux<Long> flux = Flux
                .interval(Duration.ofMillis(100))
                .log();

        flux.subscribe(e -> LOGGER.info("NUMBER {}", e));

        Thread.sleep(1000);
    }

    @Test
    void fluxSubscriberIntervalTake() throws InterruptedException {
        Flux<Long> flux = Flux
                .interval(Duration.ofMillis(100))
                .take(3)
                .log();

        flux.subscribe(e -> LOGGER.info("NUMBER {}", e));

        Thread.sleep(1000);
    }

    @Test
    void fluxSubscriberNumbersLimitRate() {
        Flux<Integer> flux = Flux
                .range(1, 10)
                .log()
                .limitRate(3);

        flux.subscribe((e) -> LOGGER.info("Number {}", e));

        StepVerifier
                .create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

    }

    @Test
    void fluxSubscriberIntervalVirtualTime() throws InterruptedException {

        StepVerifier.withVirtualTime(() -> Flux
                        .interval(Duration.ofDays(1))
                        .log())
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1))
                .thenAwait(Duration.ofDays(1))
                .expectNext(0L)
                .thenAwait(Duration.ofDays(1))
                .expectNext(1L)
                .thenCancel()
                .verify();
    }

    @Test
    void connectableFlux() throws InterruptedException {

        ConnectableFlux<Integer> publish = Flux
                .range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();

        publish.connect();

        Thread.sleep(300);

        publish.subscribe(i -> LOGGER.info("consumer 1 {}", i));

        Thread.sleep(300);

        publish.subscribe(i -> LOGGER.info("consumer 2 {}", i));

    }

    @Test
    void connectableFluxVerify() throws InterruptedException {

        ConnectableFlux<Integer> publish = Flux
                .range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();


        StepVerifier
                .create(publish)
                .then(publish::connect)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

        StepVerifier
                .create(publish)
                .then(publish::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    void connectableFluxAutoConnect() {
        Flux<Integer> flux = Flux
                .range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish()
                .autoConnect(2);

        StepVerifier
                .create(flux)
                .then(flux::subscribe)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }
}