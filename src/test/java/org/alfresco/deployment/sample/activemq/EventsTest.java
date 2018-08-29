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
package org.alfresco.deployment.sample.activemq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alfresco.deployment.sample.activemq.CamelRouteConfig.EntryEntity;
import org.alfresco.deployment.sample.activemq.CamelRouteConfig.EventTopicEntity;
import org.alfresco.event.model.EventV1;
import org.alfresco.event.model.ResourceV1;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Jamal Kaabi-Mofrad
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class EventsTest
{
    private static final String EVENT_TOPIC_API = "/api/public/events/versions/1/events";
    private static final String SEND_EVENTS_API = "/alfresco/mock/events";

    @Value("${alfresco.events.eventGateway.url}")
    private String eventGatewayUrl;

    @Value("${alfresco.mock.events.generator.url}")
    private String mockEventGeneratorUrl;

    @Autowired
    private SimpleProcessor simpleProcessor;

    private RestTemplate restTemplate;

    @Before
    public void setUp()
    {
        restTemplate = new RestTemplate();
    }

    @After
    public void tearDown()
    {
        simpleProcessor.reset();
    }

    @Test
    public void testTopicInfoRetrieval()
    {
        ResponseEntity<EntryEntity> restExchange = restTemplate.exchange(
                    getUrl(eventGatewayUrl, EVENT_TOPIC_API),
                    HttpMethod.OPTIONS,
                    null,
                    EntryEntity.class);

        assertEquals(HttpStatus.OK, restExchange.getStatusCode());

        EventTopicEntity topicEntity = restExchange.getBody().getEntry();
        assertNotNull(topicEntity);
        assertFalse("Invalid broker URL.", StringUtils.isEmpty(topicEntity.getBrokerUri()));
        assertFalse("Invalid topic name.", StringUtils.isEmpty(topicEntity.getEventTopic()));
    }

    @Test
    public void testEventsRetrieval() throws Exception
    {
        final int numOfEvents = 3;

        sendEvents(numOfEvents);

        // Wait for a few seconds to get all the messages
        TimeUnit.SECONDS.sleep(5);

        // Get the processed events
        List<Object> events = simpleProcessor.getEvents();
        assertEquals("3 Messages should have been received.", numOfEvents, events.size());

        // Check the events are public events. i.e. EventV1
        assertTrue("The event should have been of type EventV1.", events.get(0) instanceof EventV1);
        EventV1 event1 = (EventV1) events.get(0);

        assertTrue("The event should have been of type EventV1.", events.get(1) instanceof EventV1);
        EventV1 event2 = (EventV1) events.get(1);

        assertTrue("The event should have been of type EventV1.", events.get(2) instanceof EventV1);
        EventV1 event3 = (EventV1) events.get(2);

        // Check the required fields are present, per public event format
        checkEventV1Fields(event1);
        checkEventV1Fields(event2);
        checkEventV1Fields(event3);

    }

    private void sendEvents(int numOfEvents)
    {

        HttpEntity<EventRequest> request = new HttpEntity<>(new EventRequest(numOfEvents, 10L));
        ResponseEntity<EventRequest> restExchange = restTemplate.exchange(
                    getUrl(mockEventGeneratorUrl, SEND_EVENTS_API),
                    HttpMethod.POST,
                    request,
                    EventRequest.class);

        assertEquals(HttpStatus.ACCEPTED, restExchange.getStatusCode());
    }

    private String getUrl(String baseUrl, String api)
    {
        if (baseUrl.endsWith("/"))
        {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + api;
    }

    private void checkEventV1Fields(EventV1 event)
    {
        assertNotNull(event.getSchema());
        assertNotNull(event.getType());
        assertNotNull(event.getStreamPosition());
        assertNotNull(event.getPrincipal());
        assertNotNull(event.getResource());

        ResourceV1 resource = event.getResource();
        assertNotNull(resource.getSchema());
        assertNotNull(resource.getId());
    }

    public static class EventRequest
    {
        private Integer numOfEvents;
        private Long pauseTimeInMillis;

        public EventRequest(Integer numOfEvents, Long pauseTimeInMillis)
        {
            this.numOfEvents = numOfEvents;
            this.pauseTimeInMillis = pauseTimeInMillis;
        }

        public Integer getNumOfEvents()
        {
            return numOfEvents;
        }

        public void setNumOfEvents(Integer numOfEvents)
        {
            this.numOfEvents = numOfEvents;
        }

        public Long getPauseTimeInMillis()
        {
            return pauseTimeInMillis;
        }

        public void setPauseTimeInMillis(Long pauseTimeInMillis)
        {
            this.pauseTimeInMillis = pauseTimeInMillis;
        }
    }
}
