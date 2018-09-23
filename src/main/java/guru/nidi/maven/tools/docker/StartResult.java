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

public class StartResult {
    public enum State {
        OK, FAIL, WAITING
    }

    public final State state;
    public final Exception exception;

    private StartResult(State state, Exception exception) {
        this.state = state;
        this.exception = exception;
    }

    public static StartResult ok() {
        return new StartResult(State.OK, null);
    }

    public static StartResult waiting() {
        return new StartResult(State.WAITING, null);
    }

    public static StartResult fail(Exception cause) {
        return new StartResult(State.FAIL, cause);
    }
}
