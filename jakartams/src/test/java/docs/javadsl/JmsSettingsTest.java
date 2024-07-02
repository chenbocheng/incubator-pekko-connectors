/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.apache.pekko.stream.connectors.jakartams.*;
import org.apache.pekko.stream.connectors.testkit.javadsl.LogCapturingJunit4;
import org.junit.Rule;
import org.junit.Test;
import scala.Option;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

// #retry-settings #send-retry-settings

public class JmsSettingsTest {

    @Rule
    public final LogCapturingJunit4 logCapturing = new LogCapturingJunit4();

    @Test
    public void producerSettings() throws Exception {

        Config config = ConfigFactory.load();
        // #retry-settings
        Config connectionRetryConfig = config.getConfig("pekko.connectors.jakartams.connection-retry");
        // reiterating the values from reference.conf
        ConnectionRetrySettings retrySettings =
                ConnectionRetrySettings.create(connectionRetryConfig)
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withInitialRetry(Duration.ofMillis(100))
                        .withBackoffFactor(2.0)
                        .withMaxBackoff(Duration.ofMinutes(1))
                        .withMaxRetries(10);
        // #retry-settings

        ConnectionRetrySettings retrySettings2 = ConnectionRetrySettings.create(connectionRetryConfig);
        assertEquals(retrySettings.toString(), retrySettings2.toString());

        // #send-retry-settings
        Config sendRetryConfig = config.getConfig("pekko.connectors.jakartams.send-retry");
        // reiterating the values from reference.conf
        SendRetrySettings sendRetrySettings =
                SendRetrySettings.create(sendRetryConfig)
                        .withInitialRetry(Duration.ofMillis(20))
                        .withBackoffFactor(1.5d)
                        .withMaxBackoff(Duration.ofMillis(500))
                        .withMaxRetries(10);
        // #send-retry-settings
        SendRetrySettings sendRetrySettings2 = SendRetrySettings.create(sendRetryConfig);
        assertEquals(sendRetrySettings.toString(), sendRetrySettings2.toString());
        EmbeddedActiveMQResource server = new EmbeddedActiveMQResource();
        // #producer-settings
        Config producerConfig = config.getConfig(JmsProducerSettings.configPath());
        JmsProducerSettings settings =
                JmsProducerSettings.create(producerConfig, new ActiveMQConnectionFactory(server.getVmURL()))
                        .withTopic("target-topic")
                        .withCredentials(Credentials.create("username", "password"))
                        .withConnectionRetrySettings(retrySettings)
                        .withSendRetrySettings(sendRetrySettings)
                        .withSessionCount(10)
                        .withTimeToLive(Duration.ofHours(1));
        // #producer-settings
    }

    @Test
    public void consumerSettings() throws Exception {
        Config config = ConfigFactory.load();
        Config connectionRetryConfig = config.getConfig("pekko.connectors.jakartams.connection-retry");
        ConnectionRetrySettings retrySettings = ConnectionRetrySettings.create(connectionRetryConfig);
        EmbeddedActiveMQResource server = new EmbeddedActiveMQResource();

        // #consumer-settings
        Config consumerConfig = config.getConfig(JmsConsumerSettings.configPath());
        JmsConsumerSettings settings =
                JmsConsumerSettings.create(consumerConfig, new ActiveMQConnectionFactory(server.getVmURL()))
                        .withTopic("message-topic")
                        .withCredentials(Credentials.create("username", "password"))
                        .withConnectionRetrySettings(retrySettings)
                        .withSessionCount(10)
                        .withAcknowledgeMode(AcknowledgeMode.AutoAcknowledge())
                        .withSelector("Important = TRUE");
        // #consumer-settings
        assertThat(settings.sessionCount(), is(10));
        assertThat(settings.acknowledgeMode(), is(Option.apply(AcknowledgeMode.AutoAcknowledge())));
    }
}