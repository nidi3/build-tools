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
package guru.nidi.maven.tools;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.*;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static guru.nidi.maven.tools.Maps.map;
import static java.util.stream.Collectors.toList;

class DockerContainer {
    private final String image;
    private final String label;
    private static final Logger log = LoggerFactory.getLogger(DockerContainer.class);

    private final DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    private final DockerClient client = DockerClientBuilder.getInstance(config).build();

    DockerContainer(String image, String label) {
        this.image = image;
        this.label = label;
    }

    static void stop(String label) {
        new DockerContainer("", label).stop();
    }

    void start(Map<String, String> env, Map<Integer, Integer> ports, Map<String, String> cmd, Predicate<String> untilLog) {
        if (!isRunning()) {
            doStart(env, ports, cmd, untilLog);
        }
    }

    void stop() {
        if (isRunning()) {
            doStop();
        }
    }

    private boolean isRunning() {
        final List<Container> all = client.listContainersCmd().withShowAll(true).withLabelFilter(label).exec();
        final List<Container> running = client.listContainersCmd().withStatusFilter("running").withLabelFilter(label).exec();
        all.stream()
                .filter(c -> running.isEmpty() || !running.get(0).getId().equals(c.getId()))
                .forEach(c -> client.removeContainerCmd(c.getId()).withForce(true).exec());
        return !running.isEmpty();
    }

    private void doStop() {
        client.listContainersCmd().withStatusFilter("running").withLabelFilter(label).exec().stream()
                .filter(c -> c.getId() != null)
                .forEach(c -> client.stopContainerCmd(c.getId()).exec());
    }

    private void doStart(Map<String, String> env, Map<Integer, Integer> ports, Map<String, String> cmd, Predicate<String> untilLog) {
        final Semaphore ready = new Semaphore(1);
        try {
            ready.acquire();
            try {
                pullImageIfNeeded();
                startContainer(env, ports, cmd, msg -> {
                    log.info("[Docker] " + msg);
                    if (untilLog.test(msg)) {
                        ready.release();
                    }
                });
            } catch (DockerException e) {
                throw new RuntimeException("Problem starting " + image + ": " + e.getMessage(), e);
            }
            ready.tryAcquire(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("{} started", image);
    }

    private void pullImageIfNeeded() {
        final List<Image> images = client.listImagesCmd().withImageNameFilter(image).exec();
        if (images.isEmpty()) {
            log.info("Pulling {}...", image);
            final String[] parts = image.split(":");
            client.pullImageCmd(parts[0]).withTag(parts[1]).exec(new PullImageResultCallback()).awaitSuccess();
            log.info("{} pulled.", image);
        }
    }

    private void startContainer(Map<String, String> env, Map<Integer, Integer> ports, Map<String, String> cmd, Consumer<String> log) {
        final CreateContainerResponse container = client.createContainerCmd(image)
                .withCmd(cmd.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(toList()))
                .withLabels(map(label, "true"))
                .withEnv(env.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(toList()))
                .withPortBindings(ports.entrySet().stream().map(e ->
                        new PortBinding(Ports.Binding.bindPort(e.getKey()), ExposedPort.tcp(e.getValue()))).collect(toList()))
                .exec();
        client.startContainerCmd(container.getId()).exec();
        client.logContainerCmd(container.getId()).withFollowStream(true).withStdOut(true).withStdErr(true).exec(
                new LogContainerResultCallback() {
                    public void onNext(Frame frame) {
                        final String msg = new String(frame.getPayload());
                        log.accept(msg.endsWith("\n") ? msg.substring(0, msg.length() - 1) : msg);
                    }
                });
    }
}