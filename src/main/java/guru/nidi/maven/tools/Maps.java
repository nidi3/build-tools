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

import java.util.HashMap;
import java.util.Map;

class Maps {
    private Maps() {
    }

    static <K, V> Map<K, V> map(K k, V v) {
        final Map<K, V> res = new HashMap<>();
        res.put(k, v);
        return res;
    }

    static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
        final Map<K, V> res = new HashMap<>();
        res.put(k1, v1);
        res.put(k2, v2);
        return res;
    }
}
