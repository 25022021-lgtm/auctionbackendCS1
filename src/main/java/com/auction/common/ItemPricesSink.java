package com.auction.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** Component that manages reactive price sinks for real-time item price streaming. */
@Component
public class ItemPricesSink {

  private final Map<Long, Sinks.Many<Double>> itemsPricesMap;

  public ItemPricesSink() {
    itemsPricesMap = new ConcurrentHashMap<>();
  }

  /** Publishes a new price update for the given item. */
  public void publishPrice(Long itemId, Double price) {
    if (itemsPricesMap.containsKey(itemId)) {
      itemsPricesMap.get(itemId).tryEmitNext(price);
    }
  }

  /** Returns a reactive stream of price updates for the given item. */
  public Flux<Double> getPriceSink(Long itemId) {
    Sinks.Many<Double> sink =
        itemsPricesMap.computeIfAbsent(
            itemId, id -> Sinks.many().multicast().onBackpressureBuffer());

    return sink.asFlux()
        .doFinally(
            signalType -> {
              itemsPricesMap.computeIfPresent(
                  itemId,
                  (id, existingSink) -> {
                    if (existingSink.currentSubscriberCount() == 0) {
                      return null;
                    }
                    return existingSink;
                  });
            });
  }
}
