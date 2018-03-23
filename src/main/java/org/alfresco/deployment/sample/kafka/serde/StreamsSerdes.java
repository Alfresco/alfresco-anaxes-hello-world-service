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

package org.alfresco.deployment.sample.kafka.serde;

import java.util.Map;

import org.alfresco.event.model.BaseEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * @author Jamal Kaabi-Mofrad
 */
public class StreamsSerdes
{

    public static Serde<BaseEvent> BaseEventSerde()
    {
        return new BaseEventSerde();
    }

    private static final class BaseEventSerde extends WrapperSerde<BaseEvent>
    {
        private BaseEventSerde()
        {
            super(new JsonSerializer<>(), new JsonDeserializer<>());
        }
    }

    private static class WrapperSerde<T extends BaseEvent> implements Serde<T>
    {
        private JsonSerializer<T> serializer;
        private JsonDeserializer<T> deserializer;

        WrapperSerde(JsonSerializer<T> serializer, JsonDeserializer<T> deserializer)
        {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        @Override
        public void configure(Map<String, ?> map, boolean b)
        {
            //NOOP
        }

        @Override
        public void close()
        {
            //NOOP
        }

        @Override
        public Serializer<T> serializer()
        {
            return serializer;
        }

        @Override
        public Deserializer<T> deserializer()
        {
            return deserializer;
        }
    }
}
