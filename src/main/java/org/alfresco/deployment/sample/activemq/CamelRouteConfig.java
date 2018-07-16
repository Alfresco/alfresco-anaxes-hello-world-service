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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Jamal Kaabi-Mofrad
 */
@Configuration
@ConditionalOnProperty(prefix = "messaging.config", name = "enabled", havingValue = "true")
public class CamelRouteConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelRouteConfig.class);

    private static final long RETRY_BACK_OFF_PERIOD = 2000L;
    private static final int RETRY_MAX_ATTEMPTS = 30;

    @Value("${messaging.broker.activemq.url}")
    private String activemqUrl;

    @Value("${messaging.config.topic.endpoint}")
    private String topicEndpointUrl;

    @Value("${messaging.camel.from.uri}")
    private String fromUri;

    @Bean
    public RoutesBuilder simpleRoute()
    {
        return new RouteBuilder()
        {
            @Override
            public void configure()
            {
                final String topicUri = topicEndpoint().eventTopic;
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Setting camel Route to consume event from: {}", topicUri);
                }

                from(topicUri).id("HelloRoute")
                            .log("${body}");
            }
        };
    }

    @Bean
    public AMQPComponent amqpConnection()
    {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory();
        jmsConnectionFactory.setRemoteURI(topicEndpoint().brokerUri);

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(jmsConnectionFactory);

        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(cachingConnectionFactory);
        jmsConfiguration.setCacheLevelName("CACHE_CONSUMER");

        return new AMQPComponent(jmsConfiguration);
    }

    @Bean
    public RetryTemplate retryTemplate()
    {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(RETRY_BACK_OFF_PERIOD);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(RETRY_MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public EventTopicEntity topicEndpoint()
    {
        return getTopicEndpointWithRetry();
    }

    private EventTopicEntity getTopicEndpointWithRetry()
    {
        final AtomicInteger counter = new AtomicInteger(1);
        try
        {
            final RestTemplate restTemplate = new RestTemplate();
            final RetryTemplate retryTemplate = retryTemplate();
            return retryTemplate.execute(retryContext -> {
                try
                {
                    ResponseEntity<EntryEntity> restExchange = restTemplate.exchange(topicEndpointUrl,
                                HttpMethod.OPTIONS, null, EntryEntity.class);
                    EventTopicEntity topicEntity = restExchange.getBody().entry;
                    // .getEntry();
                    return new EventTopicEntity("amqpConnection:topic:" + topicEntity.eventTopic,
                                topicEntity.brokerUri);
                }
                catch (Exception ex)
                {
                    LOGGER.info("Couldn't get the topic info. Retrying using FixedBackOff [interval={}ms, maxAttempts={}, currentAttempts={}].",
                                RETRY_BACK_OFF_PERIOD, RETRY_MAX_ATTEMPTS, counter.getAndIncrement());
                    throw ex;
                }
            });
        }
        catch (Exception ex)
        {
            LOGGER.info("Couldn't get the topic info after {} tries. Falling back to default values.", RETRY_MAX_ATTEMPTS);
            return new EventTopicEntity(fromUri, activemqUrl);
        }
    }

    public static class EntryEntity
    {
        private EventTopicEntity entry;

        public EntryEntity()
        {
            //NOOP
        }

        public void setEntry(EventTopicEntity entry)
        {
            this.entry = entry;
        }
    }

    public static class EventTopicEntity
    {
        private String eventTopic;
        private String brokerUri;

        public EventTopicEntity()
        {
            //NOOP
        }

        public EventTopicEntity(String eventTopic, String brokerUri)
        {
            this.eventTopic = eventTopic;
            this.brokerUri = brokerUri;
        }

        public void setEventTopic(String eventTopic)
        {
            this.eventTopic = eventTopic;
        }

        public void setBrokerUri(String brokerUri)
        {
            this.brokerUri = brokerUri;
        }
    }
}
