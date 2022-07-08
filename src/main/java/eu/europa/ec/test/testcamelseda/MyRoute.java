package eu.europa.ec.test.testcamelseda;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
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
                .wireTap("seda:tap")
                .to("log:result");

        from("seda:tap")
                .process(sleep("tap", 3000L))
                .multicast()
                    .to("direct:a")
                    .to("direct:b")
                .end()
                .setBody().constant("Tapped")
                .to("log:tap");

        from("direct:a")
                .process(sleep("a", 1000L))
                .setBody().constant("a")
                .to("log:a");

        from("direct:b")
                .process(sleep("b", 1000L))
                .setBody().constant("b")
                .to("log:b");
    }

}
