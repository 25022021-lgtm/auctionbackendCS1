package com.auction.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ItemPricesSink {
    public final Map<Long, Sinks.Many<Double>> itemsPricesMap;

    public ItemPricesSink() {
        itemsPricesMap = new ConcurrentHashMap<Long, Sinks.Many<Double>>();
    }

    public void publishPrice(Long itemId, Double price) {
        if (itemsPricesMap.containsKey(itemId)) {
            itemsPricesMap.get(itemId).tryEmitNext(price);
        }
    }

    public Flux<Double> getPriceSink(Long itemId) {
        Sinks.Many<Double> sink = itemsPricesMap.computeIfAbsent(itemId,
                id -> Sinks.many().multicast().onBackpressureBuffer());

        return sink.asFlux()
                .doFinally(signalType -> {
                    itemsPricesMap.computeIfPresent(itemId, (id, existingSink) -> {
                        if (existingSink.currentSubscriberCount() == 0) {
                            return null;
                        }
                        return existingSink;
                    });
                });
    }

}
