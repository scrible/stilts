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

import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.Subscription;
import org.projectodd.stilts.stomplet.Stomplet;

class SubscriptionImpl implements Subscription {

    public SubscriptionImpl(Stomplet stomplet, SubscriberImpl subscriber) {
        this.stomplet = stomplet;
        this.subscriber = subscriber;
    }

    @Override
    public String getId() {
        return this.subscriber.getSubscriptionId();
    }

    @Override
    public void cancel() throws StompException {
        subscriber.close();
        stomplet.onUnsubscribe( this.subscriber );
        //Try to make things garbage collectable...
        this.subscriber = null;
        this.stomplet = null;
    }

    private Stomplet stomplet;
    private SubscriberImpl subscriber;

}
