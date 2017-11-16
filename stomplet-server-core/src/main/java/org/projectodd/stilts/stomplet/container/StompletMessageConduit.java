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

package org.projectodd.stilts.stomplet.container;

import org.jboss.logging.Logger;
import org.projectodd.stilts.conduit.spi.MessageConduit;
import org.projectodd.stilts.stomp.Headers;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.Subscription;
import org.projectodd.stilts.stomp.spi.AcknowledgeableMessageSink;
import org.projectodd.stilts.stomp.spi.StompSession;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public class StompletMessageConduit implements MessageConduit {

    private static Logger log = Logger.getLogger(StompletMessageConduit.class);
    
    public StompletMessageConduit(TransactionManager transactionManager, StompletContainer stompletContainer, AcknowledgeableMessageSink messageSink, StompSession session)
            throws StompException {
        this.transactionManager = transactionManager;
        this.stompletContainer = stompletContainer;
        this.messageSink = messageSink;
        this.session = session;
    }

    public AcknowledgeableMessageSink getMessageSink() {
        return this.messageSink;
    }
    
    public StompSession getSession() {
        return this.session;
    }

    @Override
    public void send(StompMessage message) throws StompException {
        StompletActivator activator = this.stompletContainer.getActivator( message.getDestination() );
        if (activator == null) {
            return;
        }

        try {
            Transaction tx = this.transactionManager.getTransaction();
            if (tx != null) {
                for (XAResource each : activator.getXAResources()) {
                    try {
                        tx.enlistResource( each );
                    } catch (IllegalStateException e) {
                        throw new StompException( e );
                    } catch (RollbackException e) {
                        throw new StompException( e );
                    }
                }
            }
            activator.send( message, this.session );
        } catch (SystemException e) {
            throw new StompException( e );
        }
    }

    @Override
    public Subscription subscribe(String subscriptionId, String destination, Headers headers) throws Exception {
        StompletActivator activator = this.stompletContainer.getActivator( destination );
        if (activator == null) {
            log.warnf(  "unable to find activator for destination: %s", destination );
            return null;
        }

        Subscription subscription = activator.subscribe( this, subscriptionId, destination, headers );
        //this.subscriptions.put( subscriptionId, subscription );
        return subscription;
    }

    private TransactionManager transactionManager;
    private StompletContainer stompletContainer;
    private AcknowledgeableMessageSink messageSink;
    private StompSession session;
    //private Map<String, Subscription> subscriptions = new HashMap<String, Subscription>();

}
