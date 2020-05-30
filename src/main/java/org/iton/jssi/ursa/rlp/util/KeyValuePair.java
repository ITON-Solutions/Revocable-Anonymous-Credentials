/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.iton.jssi.ursa.rlp.util;


import java.util.Arrays;

public final class KeyValuePair implements Comparable<KeyValuePair> {

     public static final KeyValuePair[] EMPTY_ARRAY = new KeyValuePair[0];

    private final byte[] key;
    private final byte[] value;

    public KeyValuePair(String key, String value, int valueEncoding) {
        this.key = Strings.decode(key);
        this.value = Strings.decode(value, valueEncoding);
    }

    public KeyValuePair(byte[] key, byte[] value) {
        this.key = Arrays.copyOf(key, key.length);
        this.value = Arrays.copyOf(value, value.length);
    }

    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }

    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValuePair that = (KeyValuePair) o;
        return Arrays.equals(key, that.key);
    }

    @Override
    public String toString() {
        return Strings.encode(key) + " --> " + Strings.encode(value, Strings.HEX);
    }

    @Override
    public int compareTo(KeyValuePair o) {
        int result = compare(key, o.key);
        if (result == 0) {
            throw new IllegalArgumentException("duplicate key: " + Strings.encode(o.key));
        }
        return result;
    }

    private static int compare(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        final int len = Math.min(a.length, b.length);
        int i = 0;
        boolean mismatch = false;
        for ( ; i < len; i++) {
            if (a[i] != b[i]) {
                mismatch = true;
                break;
            }
        }
        return mismatch ? a[i] - b[i] : a.length - b.length;
    }
}
