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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RestController
@RequestMapping("/hello")
public class HelloController
{
    private static final Log logger = LogFactory.getLog(HelloController.class);
    
    @Value("${alfresco.dbp.url}")
    private String dbpUrl;
    
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
        
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken)request.getUserPrincipal();
        KeycloakSecurityContext context = (KeycloakSecurityContext)token.getCredentials();
        
        if (logger.isDebugEnabled())
            logger.debug("jwt: " + context.getTokenString());
        
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
}
