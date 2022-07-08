package eu.europa.ec.test.testcamelseda;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/routes")
public class CamelController {

    @Autowired
    protected CamelContext camelContext;

    @PostMapping(path = "/{uri}")
    public void postRoute(@PathVariable("uri") String uri, @RequestBody String body) {
        ProducerTemplate template = camelContext.createProducerTemplate();
        template.sendBody(uri, body);
    }

}
