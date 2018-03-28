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

package org.alfresco.deployment.sample.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.alfresco.deployment.sample.kafka.serde.StreamsSerdes;
import org.alfresco.event.model.BaseEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Jamal Kaabi-Mofrad
 */
@Component
public class KafkaStreamsConsumerApp
{
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStreamsConsumerApp.class);

    @Value("${messaging.kafka.applicationId}")
    private String applicationId;
    @Value("${messaging.kafka.bootstrapServers}")
    private String bootstrapServers;
    @Value("${messaging.kafka.sourceTopics}")
    private String sourceTopics;

    public void start()
    {
        StreamsConfig streamsConfig = new StreamsConfig(getStreamsConfigProperties());

        Serde<BaseEvent> eventSerde = StreamsSerdes.BaseEventSerde();
        Serde<String> stringSerde = Serdes.String();

        StreamsBuilder builder = new StreamsBuilder();

        List<String> topics = getTopics();
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Subscribing to topics: " + topics);
        }

        KStream<String, BaseEvent> baseEventStream = builder.stream(topics, Consumed.with(stringSerde, eventSerde));
        baseEventStream.foreach((key, value) -> LOGGER.info("[Event] key: {}, value: {}" , key, value));

        KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsConfig);
        LOGGER.info("Starting KafkaStreamsConsumerApp...");
        kafkaStreams.start();
    }

    private Properties getStreamsConfigProperties()
    {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    private List<String> getTopics()
    {
        return Arrays.asList(sourceTopics.split(","));
    }
}
