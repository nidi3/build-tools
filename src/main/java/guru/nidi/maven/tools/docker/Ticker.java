/*
 * Copyright © 2014 Stefan Niederhauser (nidin@gmx.ch)
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
package guru.nidi.maven.tools.docker;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

class Ticker implements Runnable, AutoCloseable {
    private final int period;
    private final long totalTimeout;
    private final BiFunction<String, Long, StartResult> waiter;
    private final Semaphore ready = new Semaphore(1);
    private volatile StartResult result = StartResult.waiting();
    private String message = "";
    private long last;

    Ticker(int period, long totalTimeout, BiFunction<String, Long, StartResult> waiter) {
        this.period = period;
        this.totalTimeout = totalTimeout;
        this.waiter = waiter;
        try {
            ready.acquire();
            final Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (result.state == StartResult.State.WAITING) {
            long diff = System.currentTimeMillis() - last;
            if (diff >= period) {
                handle(message, diff);
            }
            try {
                Thread.sleep(period - (diff % period));
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    void handle(String msg) {
        handle(msg, 0);
    }

    private void handle(String msg, long since) {
        message = msg;
        if (since == 0) {
            last = System.currentTimeMillis();
        }
        try {
            result = waiter.apply(message, since);
        } catch (Exception e) {
            result = StartResult.fail(e);
        }
        if (result.state != StartResult.State.WAITING) {
            ready.release();
        }
    }

    StartResult waitFor() {
        try {
            ready.tryAcquire(totalTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //ignore
        } finally {
            close();
        }
        return result;
    }

    @Override
    public void close() {
        if (result.state == StartResult.State.WAITING) {
            result = StartResult.fail(new RuntimeException("Waiting for too long"));
        }
    }
}
