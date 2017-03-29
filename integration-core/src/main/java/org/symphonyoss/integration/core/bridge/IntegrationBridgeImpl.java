/**
 * Copyright 2016-2017 Symphony Integrations - Symphony LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.symphonyoss.integration.core.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.Integration;
import org.symphonyoss.integration.authentication.AuthenticationProxy;
import org.symphonyoss.integration.core.bootstrap.IntegrationBootstrapContext;
import org.symphonyoss.integration.exception.RemoteApiException;
import org.symphonyoss.integration.exception.authentication.ConnectivityException;
import org.symphonyoss.integration.model.config.IntegrationInstance;
import org.symphonyoss.integration.model.message.Message;
import org.symphonyoss.integration.service.IntegrationBridge;
import org.symphonyoss.integration.service.StreamService;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ProcessingException;

/**
 * See @{@link IntegrationBridge} for further details.
 */
@Component
public class IntegrationBridgeImpl implements IntegrationBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationBridgeImpl.class);

  @Autowired
  private IntegrationBootstrapContext bootstrap;

  @Autowired
  private StreamService streamService;

  @Autowired
  private IntegrationBridgeExceptionHandler exceptionHandler;

  @Override
  public List<Message> sendMessage(IntegrationInstance instance, String integrationUser, Message message) {
    List<Message> result = new ArrayList<>();
    List<String> streams = streamService.getStreams(instance);

    if (streams.isEmpty()) {
      LOGGER.info("No streams configured to instance {}", instance.getInstanceId());
      return result;
    }

    return sendMessage(instance, integrationUser, streams, message);
  }

  @Override
  public List<Message> sendMessage(IntegrationInstance instance, String integrationUser,
      List<String> streams, Message message) {
    List<Message> result = new ArrayList<>();

    for (String stream : streams) {
      try {
        Message messageResponse = postMessage(integrationUser, stream, message);
        result.add(messageResponse);
      } catch (RemoteApiException e) {
        exceptionHandler.handleRemoteApiException(e, instance, integrationUser, message, stream);
      } catch (ConnectivityException | ProcessingException e) {
        throw e;
      } catch (Exception e) {
        exceptionHandler.handleUnexpectedException(e);
      }
    }

    return result;
  }

  /**
   * Sends a message to a specific stream using {@link AuthenticationProxy}.
   * @param integrationUser the user of integration
   * @param stream the stream identifier.
   * @param message the actual message. It's expected to be already on proper format.
   * @return Response message
   * @throws RemoteApiException
   */
  private Message postMessage(String integrationUser, String stream, Message message)
      throws RemoteApiException {
    Message messageResponse = streamService.postMessage(integrationUser, stream, message);
    LOGGER.info("Message posted to stream {} ", stream);

    return messageResponse;
  }

  @Override
  public Integration getIntegrationById(String integrationId) {
    return this.bootstrap.getIntegrationById(integrationId);
  }

  @Override
  public void removeIntegration(String integrationId) {
    this.bootstrap.removeIntegration(integrationId);
  }

}
