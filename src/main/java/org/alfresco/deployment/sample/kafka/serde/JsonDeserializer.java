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

import java.io.IOException;
import java.util.Map;

import org.alfresco.event.databind.EventObjectMapperFactory;
import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jamal Kaabi-Mofrad
 */
public class JsonDeserializer<T> implements Deserializer<T>
{
    private ObjectMapper mapper;

    public JsonDeserializer()
    {
        mapper = EventObjectMapperFactory.createInstance();
    }

    @Override
    public void configure(Map<String, ?> map, boolean b)
    {
        //NOOP
    }

    @Override
    public T deserialize(String topic, byte[] bytes)
    {
        if (bytes == null || bytes.length == 0)
        {
            return null;
        }

        try
        {
            return mapper.readValue(bytes, new TypeReference<T>()
            {
            });
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close()
    {
        //NOOP
    }
}
