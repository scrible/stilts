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

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.logging.Logger;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomplet.Subscriber;

public class SubscriberList {

    private static Logger log = Logger.getLogger(SubscriberList.class);
    private static boolean perfWarned = false;
    
    public SubscriberList() {
    }

    public int size() {
        return this.subscribers.size();
    }

    public void addSubscriber(Subscriber subscriber) {
        this.subscribers.add( subscriber );
    }

    public boolean removeSubscriber(Subscriber subscriber) {
        return this.subscribers.remove( subscriber );
    }

    protected void sendToAllSubscribers(StompMessage message) throws StompException {
        Iterator<Subscriber> iter = subscribers.iterator();
        while (iter.hasNext()) {
    		Subscriber each = iter.next();
        	try {
        		each.send( message );
        	} catch (StompException se) {
        		log.warn("다음 subscriber에게 메시지를 전달하는 중 예외가 발생하였습니다. subscriber: " + each, se);
        		iter.remove();
        	}
        }
    }

    protected synchronized void sendToOneSubscriber(StompMessage message) throws StompException {
    	// sendToOneSubscriber()는 사용되지 않을 것이라 가정하고 sendToAllSubscribers의 lock-free 구현을 위해
    	// ConcurrentLinkedQueue를 사용함
    	if (!perfWarned) {
        	log.warn("비효율적으로 구현된 메소드가 호출되었습니다. 이 메소드를 사용하려면 구현을 변경하세요!");	
    		perfWarned = true;
    	}
        int luckyWinner = this.random.nextInt( this.subscribers.size() );
        Iterator<Subscriber> iter = subscribers.iterator();
        int i = 0;
        Subscriber subscriber = null;
        while (i < luckyWinner && iter.hasNext()) {
        	subscriber = iter.next();
        }
        if (subscriber != null) {
        	subscriber.send( message );
        }
    }

    private final Collection<Subscriber> subscribers = new ConcurrentLinkedQueue<Subscriber>();
    private Random random = new Random( System.currentTimeMillis() );

}
