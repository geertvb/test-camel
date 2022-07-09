package eu.europa.ec.test.testcamelseda;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;

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
                .completionTimeout(10000L).to("log:result");
    }

}
