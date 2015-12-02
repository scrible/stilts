package org.projectodd.stilts.stomp;

import org.jboss.logging.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Heartbeat {

    private static final double TOLERANCE_PERCENTAGE = 0.05;
    private static final Logger log = Logger.getLogger( Heartbeat.class );

    public Heartbeat() {
    }

    public void start(final Runnable callback) {
        final int duration = calculateDuration(serverSend, clientReceive);
        log.debugf("heartbeat duration: %d", duration);
        if (duration > 0) {
            future = heartbeatScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    long lastUpdate = getLastUpdate();
                    long diff = now - lastUpdate;
                    double tolerance = duration * TOLERANCE_PERCENTAGE;

                    if (diff > duration - tolerance) {
                        log.tracef("HEARTBEAT : %s / %s", diff, duration);
                        try {
                            callback.run();
                        } catch (Exception e) {
                            log.error("Could not send heartbeat message:", e);
                        }
                        touch();
                    }
                    long targetDelay = lastUpdate + duration - now;
                    if (!stopped) {
                        future = heartbeatScheduler.schedule(this, targetDelay, TimeUnit.MILLISECONDS);
                    }
                }
            }, duration, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        stopped = true;
        Future<?> future = this.future;
        if (future != null) {
            future.cancel(false);
        }
    }

    public int calculateDuration(int senderDuration, int receiverDuration) {
        if (senderDuration == 0 || receiverDuration == 0) {
            return 0;
        }
        return Math.max( senderDuration, receiverDuration );
    }

    public int getClientReceive() {
        return clientReceive;
    }

    public int getClientSend() {
        return clientSend;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public int getServerReceive() {
        return serverReceive;
    }

    public int getServerSend() {
        return serverSend;
    }

    public void setClientReceive(int clientReceive) {
        this.clientReceive = clientReceive;
    }

    public void setClientSend(int clientSend) {
        this.clientSend = clientSend;
    }

    public void setServerReceive(int serverReceive) {
        this.serverReceive = serverReceive;
    }

    public void setServerSend(int serverSend) {
        this.serverSend = serverSend;
    }

    public synchronized void touch() {
        lastUpdate = System.currentTimeMillis();
    }

    public Future<?> getFuture() {
        return future;
    }

    private int clientSend;
    private int clientReceive;
    private int serverSend = 1000;
    private int serverReceive = 1000;
    private long lastUpdate = System.currentTimeMillis();
    private volatile boolean stopped = false;
    private volatile Future<?> future;
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(4);
}
