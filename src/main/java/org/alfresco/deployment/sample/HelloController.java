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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@RestController
@RequestMapping("/hello")
public class HelloController
{
    private static final Log logger = LogFactory.getLog(HelloController.class);
    
    private static final String CLIENT_ID_PARAM = "client_id";
    private static final String GRANT_TYPE_PARAM = "grant_type";
    private static final String USERNAME_PARAM = "username";
    private static final String PASSWORD_PARAM = "password";
    
    private static final String CLIENT_ACTIVITI = "activiti";
    private static final String GRANT_TYPE = "password";
    
    private static final String ACCESS_TOKEN_PROPERTY = "access_token";
    
    @Value("${alfresco.dbp.url}")
    private String dbpUrl;
    
    @Value("${alfresco.dbp.username}")
    private String dbpUsername;
    
    @Value("${alfresco.dbp.password}")
    private String dbpPassword;
    
    @Autowired
    private HelloTextRepository helloTextRepository;
    
    private String jwt;

    @RequestMapping(path = "/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HelloText> getHelloText(@PathVariable String key)
    {
        HelloText helloText = helloTextRepository.findOne(key);
        if (helloText == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // request JWT if DBP URL has been provided
        if (this.dbpUrl != null && !this.dbpUrl.isEmpty())
        {
            requestAuthToken();
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
    
    private void requestAuthToken()
    {
        // don't attempt to get another JWT if we already have one!
        if (this.jwt == null)
        {
            String tokenUrl = this.dbpUrl + "/auth/realms/springboot/protocol/openid-connect/token";
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Requesting JWT from " + tokenUrl);
                logger.debug("DBP username: " + this.dbpUsername);
                logger.debug("DBP password: " + this.dbpPassword);
            }
            
            int TIMEOUT = 2000;
           
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();)
            {
                // configure request with fairly short timeout
                HttpPost postRequest = new HttpPost(tokenUrl);
                RequestConfig config = RequestConfig.custom()
                            .setSocketTimeout(TIMEOUT)
                            .setConnectionRequestTimeout(TIMEOUT)
                            .setConnectTimeout(TIMEOUT).build();
                postRequest.setConfig(config);
                
                // build parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair(CLIENT_ID_PARAM, CLIENT_ACTIVITI));
                params.add(new BasicNameValuePair(GRANT_TYPE_PARAM, GRANT_TYPE));
                params.add(new BasicNameValuePair(USERNAME_PARAM, this.dbpUsername));
                params.add(new BasicNameValuePair(PASSWORD_PARAM, this.dbpPassword));
                postRequest.setEntity(new UrlEncodedFormEntity(params));
                
                // execute the request
                CloseableHttpResponse response = httpClient.execute(postRequest);
                
                if (logger.isDebugEnabled())
                    logger.debug("response code " + response.getStatusLine().getStatusCode());
                
                // extract the JWT from the response
                if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK)
                {
                    HttpEntity entity = response.getEntity();
                    if (entity != null)
                    {
                        String responseBody = EntityUtils.toString(entity);
                        
                        if (logger.isTraceEnabled())
                            logger.trace("response body: " + responseBody);
                        
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readValue(responseBody, JsonNode.class);
                        
                        if (jsonNode.has(ACCESS_TOKEN_PROPERTY))
                        {
                            this.jwt = jsonNode.get(ACCESS_TOKEN_PROPERTY).asText();
                            logger.info("DBP JWT: " + this.jwt);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Failed to obtain JWT: ", e);
            }
        }
    }
}
