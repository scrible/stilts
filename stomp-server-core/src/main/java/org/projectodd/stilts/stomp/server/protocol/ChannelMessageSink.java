/*
 * Copyright 2011 Red Hat, Inc, and individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.stilts.stomp.server.protocol;

import java.io.IOException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.DefaultExceptionEvent;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.TransactionalAcknowledger;
import org.projectodd.stilts.stomp.spi.TransactionalAcknowledgeableMessageSink;

public class ChannelMessageSink implements TransactionalAcknowledgeableMessageSink {

    public ChannelMessageSink(Channel channel, AckManager ackManager) {
        this.channel = channel;
        this.ackManager = ackManager;
    }

    @Override
    public void send(StompMessage message) throws StompException {
        send( message, null );
    }

    @Override
    public void send(StompMessage message, TransactionalAcknowledger acknowledger) throws StompException {
        if (acknowledger != null) {
            this.ackManager.registerAcknowledger( message.getId(), acknowledger );
        }

        if (this.channel.isWritable()) {
        	notWritableSince = Long.MAX_VALUE;
        } else {
        	// channel is not writable
        	long now = System.currentTimeMillis();
        	if (notWritableSince == Long.MAX_VALUE) {
        		notWritableSince = now;
        	} else if (now - notWritableSince >  TEN_SECONDS_MS) {
        		IOException ioException = new IOException("channel is not writable for 10 seconds!");
        		this.channel.getPipeline().sendUpstream(new DefaultExceptionEvent(channel, ioException));
        	}
        }
        this.channel.write( message );
    }
    
    private Channel channel;
    private AckManager ackManager;
    private long notWritableSince = Long.MAX_VALUE;
    
    private static long TEN_SECONDS_MS = 10 * 1000;
    
}
