package eu.europa.ec.test.testcamelseda;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.camel.ExchangePropertyKey.GROUPED_EXCHANGE;

@Slf4j
@Component
public class MyRoute extends RouteBuilder {

    protected Processor sleep(String label, long millis) {
        return (exchange) -> {
            log.info("Sleep {}", label);
            Thread.sleep(millis);
            log.info("Wake {}", label);
        };
    }

    public Predicate finished(String... routes) {
        return exchange -> {
            List<Message> messages = exchange.getProperty(GROUPED_EXCHANGE, List.class);
            List<String> bodies = messages.stream()
                    .map(Message::getExchange)
                    .map(Exchange::getIn)
                    .map(Message::getBody)
                    .map(String.class::cast)
                    .toList();
            for (String route : routes) {
                if (!bodies.contains(route)) {
                    return false;
                }
            }
            return true;
        };
    }

    @Override
    public void configure() {
        from("direct:start")
                .setProperty("correlationId", simple("${id}"))
                .wireTap("seda:tap")
                .to("log:result");

        from("seda:tap")
                .process(sleep("tap", 3000L))
                .multicast()
                .to("direct:a")
                .to("direct:b")
                .end()
                .setBody().constant("Tapped")
                .to("direct:result");

        from("direct:a")
                .process(sleep("a", 1000L))
                .setBody().constant("a")
                .to("direct:result");

        from("direct:b")
                .process(sleep("b", 1000L))
                .setBody().constant("b")
                .to("direct:result");

        from("direct:result")
                .aggregate(new GroupedMessageAggregationStrategy())
                .simple("${exchangeProperty.correlationId}")
                //.completionSize(3)
                .completionPredicate(finished("Tapped"))
                .completionTimeout(10000L).to("log:result");
    }

}
