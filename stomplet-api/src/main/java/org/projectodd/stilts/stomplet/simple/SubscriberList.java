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

package org.projectodd.stilts.stomplet.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jboss.logging.Logger;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomplet.Subscriber;

public class SubscriberList {

    private static Logger log = Logger.getLogger(SubscriberList.class);
    
    public SubscriberList() {
    }

    public synchronized int size() {
        return this.subscribers.size();
    }

    public synchronized void addSubscriber(Subscriber subscriber) {
        this.subscribers.add( subscriber );
    }

    public synchronized boolean removeSubscriber(Subscriber subscriber) {
        return this.subscribers.remove( subscriber );
    }

    protected synchronized void sendToAllSubscribers(StompMessage message) throws StompException {
        for (Subscriber each : this.subscribers) {
        	try {
        		each.send( message );
        	} catch (StompException se) {
        		log.warn("다음 subscriber에게 메시지를 전달하는 중 예외가 발생하였습니다. subscriber: " + each, se);
        		removeSubscriber(each);
        	}
        }
    }

    protected synchronized void sendToOneSubscriber(StompMessage message) throws StompException {
        int luckyWinner = this.random.nextInt( this.subscribers.size() );
        this.subscribers.get( luckyWinner ).send( message );
    }

    private final List<Subscriber> subscribers = new ArrayList<Subscriber>();
    private Random random = new Random( System.currentTimeMillis() );

}
