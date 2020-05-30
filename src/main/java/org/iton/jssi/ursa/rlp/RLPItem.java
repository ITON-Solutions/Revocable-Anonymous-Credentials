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
package org.iton.jssi.ursa.rlp;

import org.iton.jssi.ursa.rlp.util.Integers;
import org.iton.jssi.ursa.rlp.util.Notation;
import org.iton.jssi.ursa.rlp.util.Strings;

import java.util.Arrays;

/**
 * An immutable view of a portion of a byte array containing RLP-encoded data, starting at {@code index} (inclusive) and
 * ending at {@code endIndex} (exclusive), representing a single item (either a string or list). Useful when decoding or
 * otherwise manipulating RLP items.
 *
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem {

    public static final RLPItem[] EMPTY_ARRAY = new RLPItem[0];

    protected final byte[] buffer;
    protected final int index;

    public final transient int dataIndex;
    public final transient int dataLength;
    public final transient int endIndex;

    RLPItem(byte lead, RLPDataType type, byte[] buffer, int index, int containerEnd, boolean lenient) throws DecodeException {
        containerEnd = Math.min(buffer.length, containerEnd);

        final int _dataIndex;
        final long _dataLength;
        
        final int diff = lead - type.offset;
        switch (type) {
        case SINGLE_BYTE: _dataIndex = index; _dataLength = 1; break;
        case STRING_SHORT:
        case LIST_SHORT:
            _dataIndex = index + 1; _dataLength = diff;
            break;
        case STRING_LONG:
        case LIST_LONG:
            int lengthIndex = index + 1;
            _dataIndex = lengthIndex + diff; // RLPDataType dictates that lengthOfLength guaranteed to be in [1,8]
            if (_dataIndex > containerEnd) {
                throw exceedsContainer(index, _dataIndex, containerEnd, containerEnd == buffer.length);
            }
            _dataLength = Integers.getLong(buffer, lengthIndex, diff);
            if(_dataLength < RLPDataType.MIN_LONG_DATA_LEN) {
                throw new UnrecoverableDecodeException("long element data length must be " + RLPDataType.MIN_LONG_DATA_LEN + " or greater; found: " + _dataLength + " for element @ " + index);
            }
            break;
        default: throw new RuntimeException();
        }

        final long _endIndex = _dataIndex + _dataLength;

        if(_endIndex > containerEnd) {
            throw exceedsContainer(index, _dataIndex, containerEnd, containerEnd == buffer.length);
        }
        if(!lenient && _dataLength == 1 && type == RLPDataType.STRING_SHORT && buffer[_dataIndex] >= 0x00) { // same as (data[from] & 0xFF) < 0x80
            throw new UnrecoverableDecodeException("invalid rlp for single byte @ " + index);
        }

        this.buffer = buffer;
        this.index = index;
        this.dataIndex = _dataIndex;
        this.dataLength = (int) _dataLength;
        this.endIndex = (int) _endIndex;
    }

    static DecodeException exceedsContainer(int index, int end, int containerEnd, boolean recoverable) {
        String msg = "element @ index " + index + " exceeds its container: " + end + " > " + containerEnd;
        return recoverable ? new RecoverableDecodeException(msg) : new UnrecoverableDecodeException(msg);
    }

    public final RLPDataType type() {
        return RLPDataType.type(buffer[index]);
    }

    public abstract boolean isList();

    public final int encodingLength() {
        return endIndex - index;
    }

    public final byte[] encoding() {
        final int len = encodingLength();
        byte[] copy = new byte[len];
        System.arraycopy(buffer, index, copy, 0, len);
        return copy;
    }

    public final byte[] data() {
        byte[] copy = new byte[dataLength];
        System.arraycopy(buffer, dataIndex, copy, 0, dataLength);
        return copy;
    }

    public final byte[] bytes() {
        byte[] copy = new byte[buffer.length];
        System.arraycopy(buffer, 0, copy, 0, buffer.length);
        return copy;
    }

    public final byte[] copyOfRange(int from, int to) {
        checkRangeBounds(from, to);
        final int len = to - from;
        byte[] range = new byte[len];
        System.arraycopy(buffer, from, range, 0, len);
        return range;
    }

    public final int export(byte[] dest, int destIndex) {
        return exportRange(index, endIndex, dest, destIndex);
    }

    public final int exportData(byte[] dest, int destIndex) {
        return exportRange(dataIndex, endIndex, dest, destIndex);
    }

    public final int exportRange(int from, int to, byte[] dest, int destIndex) {
        checkRangeBounds(from, to);
        int len = to - from;
        System.arraycopy(buffer, from, dest, destIndex, len);
        return destIndex + len;
    }

    private void checkRangeBounds(int from, int to) {
        if(from < dataIndex) {
            throw new IndexOutOfBoundsException(from + " < " + dataIndex);
        }
        if(to > endIndex) {
            throw new IndexOutOfBoundsException(to + " > " + endIndex);
        }
    }

    /**
     * Clones this object.
     *
     * @param decoder either {@link RLPDecoder#RLP_STRICT} or {@link RLPDecoder#RLP_LENIENT}
     * @return  an independent and exact copy
     * @throws DecodeException  if an unexpected problem in decoding occurs
     */
    public abstract RLPItem duplicate(RLPDecoder decoder) throws DecodeException;

    /**
     * @see Arrays#hashCode(byte[])
     */
    @Override
    public final int hashCode() {
        int result = 1;
        final byte[] buf = buffer;
        final int end = endIndex;
        for (int i = index; i < end; i++) {
            result = 31 * result + buf[i];
        }
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if(!(obj instanceof RLPItem)) {
            return false;
        }

        RLPItem other = (RLPItem) obj;

        final int length = this.endIndex - this.index;
        if(length != other.endIndex - other.index) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (this.buffer[this.index + i] != other.buffer[other.index + i])
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        try {
            return Notation.forEncoding(buffer, index, endIndex).toString();
        } catch (DecodeException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString(int encoding) {
        return Strings.encode(buffer, index, encodingLength(), encoding);
    }
}
