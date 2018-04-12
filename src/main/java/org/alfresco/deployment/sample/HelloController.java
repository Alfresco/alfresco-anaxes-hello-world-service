/*
 * Copyright 2018 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.deployment.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@RestController
@RequestMapping("/hello")
public class HelloController
{
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private HelloTextRepository helloTextRepository;

    @RequestMapping(path = "/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HelloText> getHelloText(@PathVariable String key)
    {
        HelloText helloText = helloTextRepository.findOne(key);
        if (helloText == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<HelloText>(helloText, HttpStatus.OK);
                
    }

    @RequestMapping(method = RequestMethod.POST, 
            consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<HelloText> createHelloText(@RequestBody HelloText helloText)
    {
        return new ResponseEntity<HelloText>(helloTextRepository.save(helloText), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/{key}", method = RequestMethod.PUT, 
            consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<HelloText> updateHelloText(@RequestBody HelloText helloText)
    {
        return new ResponseEntity<HelloText>(helloTextRepository.save(helloText), HttpStatus.OK);
    }

    @RequestMapping(path = "/{key}", method = RequestMethod.DELETE)
    public ResponseEntity<?> updateHelloText(@PathVariable String key)
    {
        try
        {
            helloTextRepository.delete(key);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @RequestMapping(path = "/aps", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getApsEndpoint(RestTemplate restTemplate)
    {
        String theUrl = "http://localhost:8080/activiti-app/api/enterprise/app-version";
        HttpHeaders headers = createHttpHeaders("admin","admin");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        ApsEndpoint about = restTemplate.exchange(theUrl, HttpMethod.GET, entity, ApsEndpoint.class).getBody();
        return new ResponseEntity<String>(about.toString(), HttpStatus.OK);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    private HttpHeaders createHttpHeaders(String user, String password)
    {
        String notEncoded = user + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(notEncoded.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Basic " + encodedAuth);
        return headers;
    }
}
