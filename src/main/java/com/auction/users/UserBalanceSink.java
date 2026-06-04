package com.auction.users;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class UserBalanceSink {

    private final Map<String, Sinks.Many<Double>> balanceSinksMap;

    public UserBalanceSink() {
        this.balanceSinksMap = new ConcurrentHashMap<
            String,
            Sinks.Many<Double>
        >();
    }

    public void pushNewBalance(String username, Double balance) {
        Sinks.Many<Double> sink = balanceSinksMap.get(username);
        if (sink != null) {
            sink.tryEmitNext(balance);
        }
    }

    public Flux<Double> getBalanceSink(String username) {
        Sinks.Many<Double> sink = balanceSinksMap.computeIfAbsent(
            username,
            id -> Sinks.many().multicast().onBackpressureBuffer()
        );
        return sink.asFlux().doFinally(signalType -> {
            balanceSinksMap.computeIfPresent(username, (id, existingSink) -> {
                if (existingSink.currentSubscriberCount() == 0) {
                    return null;
                }
                return existingSink;
            });
        });
    }
}
