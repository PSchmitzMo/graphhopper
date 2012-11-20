/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.coll;

import com.graphhopper.util.Helper;

/**
 * Copied from Android project. android.util.LongSparseArray.java
 *
 * SparseArrays map longs to Objects. Unlike a normal array of Objects, there can be gaps in the
 * indices. It is intended to be more efficient than using a HashMap to map Longs to Objects.
 */
public class SparseLongLongArray {

    private static final long DELETED = -2L;
    private boolean mGarbage = false;

    /**
     * Creates a new SparseLongLongArray containing no mappings.
     */
    public SparseLongLongArray() {
        this(10);
    }

    /**
     * Creates a new SparseLongLongArray containing no mappings that will not require any additional
     * memory allocation to store the specified number of mappings.
     */
    public SparseLongLongArray(int cap) {
        try {
            cap = Helper.idealIntArraySize(cap);
            mKeys = new long[cap];
            mValues = new long[cap];
            mSize = 0;
        } catch (OutOfMemoryError err) {
            System.err.println("requested capacity " + cap);
            throw err;
        }
    }

    /**
     * @return A copy of all keys contained in the sparse array.
     */
    private long[] getKeys() {
        int length = mKeys.length;
        long[] result = new long[length];
        System.arraycopy(mKeys, 0, result, 0, length);
        return result;
    }

    /**
     * Sets all supplied keys to the given unique value.
     *
     * @param keys Keys to set
     * @param uniqueValue Value to set all supplied keys to
     */
    private void setValues(long[] keys, long uniqueValue) {
        int length = keys.length;
        for (int i = 0; i < length; i++) {
            put(keys[i], uniqueValue);
        }
    }

    /**
     * Gets the Object mapped from the specified key, or
     * <code>null</code> if no such mapping has been made.
     */
    public long get(long key) {
        return get(key, -1);
    }

    /**
     * Gets the Object mapped from the specified key, or the specified Object if no such mapping has
     * been made.
     */
    private long get(long key, long valueIfKeyNotFound) {
        int i = binarySearch(mKeys, 0, mSize, key);
        if (i < 0 || mValues[i] == DELETED) {
            return valueIfKeyNotFound;
        } else {
            return mValues[i];
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    public void delete(long key) {
        int i = binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            if (mValues[i] != DELETED) {
                mValues[i] = DELETED;
                mGarbage = true;
            }
        }
    }

    /**
     * Alias for {@link #delete(long)}.
     */
    public void remove(long key) {
        delete(key);
    }

    private void gc() {
        int n = mSize;
        int o = 0;
        long[] keys = mKeys;
        long[] values = mValues;

        for (int i = 0; i < n; i++) {
            long val = values[i];

            if (val != DELETED) {
                if (i != o) {
                    keys[o] = keys[i];
                    values[o] = val;
                }

                o++;
            }
        }

        mGarbage = false;
        mSize = o;
    }

    /**
     * Adds a mapping from the specified key to the specified value, replacing the previous mapping
     * from the specified key if there was one.
     */
    public int put(long key, long value) {
        int i = binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            mValues[i] = value;
        } else {
            i = ~i;

            if (i < mSize && mValues[i] == DELETED) {
                mKeys[i] = key;
                mValues[i] = value;
                return i;
            }

            if (mGarbage && mSize >= mKeys.length) {
                gc();

                // Search again because indices may have changed.
                i = ~binarySearch(mKeys, 0, mSize, key);
            }

            if (mSize >= mKeys.length) {
                int n = Helper.idealIntArraySize(mSize + 1);

                long[] nkeys = new long[n];
                long[] nvalues = new long[n];

                System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

                mKeys = nkeys;
                mValues = nvalues;
            }

            if (mSize - i != 0) {
                System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
                System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
            }

            mKeys[i] = key;
            mValues[i] = value;
            mSize++;
        }
        return i;
    }

    /**
     * Returns the number of key-value mappings that this SparseLongLongArray currently stores.
     */
    public int size() {
        if (mGarbage) {
            gc();
        }

        return mSize;
    }

    /**
     * Given an index in the range
     * <code>0...size()-1</code>, returns the key from the
     * <code>index</code>th key-value mapping that this SparseLongLongArray stores.
     */
    public long keyAt(int index) {
        if (mGarbage) {
            gc();
        }

        return mKeys[index];
    }

    /**
     * Given an index in the range
     * <code>0...size()-1</code>, sets a new key for the
     * <code>index</code>th key-value mapping that this SparseLongLongArray stores.
     */
    public void setKeyAt(int index, long key) {
        if (mGarbage) {
            gc();
        }

        mKeys[index] = key;
    }

    /**
     * Given an index in the range
     * <code>0...size()-1</code>, returns the value from the
     * <code>index</code>th key-value mapping that this SparseLongLongArray stores.
     */
    public long valueAt(int index) {
        if (mGarbage) {
            gc();
        }

        return mValues[index];
    }

    /**
     * Given an index in the range
     * <code>0...size()-1</code>, sets a new value for the
     * <code>index</code>th key-value mapping that this SparseLongLongArray stores.
     */
    public void setValueAt(int index, long value) {
        if (mGarbage) {
            gc();
        }

        mValues[index] = value;
    }

    /**
     * Returns the index for which {@link #keyAt} would return the specified key, or a negative
     * number if the specified key is not mapped.
     */
    private int indexOfKey(long key) {
        if (mGarbage) {
            gc();
        }

        return binarySearch(mKeys, 0, mSize, key);
    }

    /**
     * Returns an index for which {@link #valueAt} would return the specified key, or a negative
     * number if no keys map to the specified value. Beware that this is a linear search, unlike
     * lookups by key, and that multiple keys can map to the same value and this will find only one
     * of them.
     */
    private int indexOfValue(long value) {
        if (mGarbage) {
            gc();
        }

        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value)
                return i;
        }

        return -1;
    }

    /**
     * Removes all key-value mappings from this SparseLongLongArray.
     */
    public void clear() {
        int n = mSize;
        long[] values = mValues;

        for (int i = 0; i < n; i++) {
            values[i] = -1;
        }

        mSize = 0;
        mGarbage = false;
    }

    /**
     * Puts a key/value pair into the array, optimizing for the case where the key is greater than
     * all existing keys in the array.
     */
    public int append(long key, long value) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            return put(key, value);
        }

        if (mGarbage && mSize >= mKeys.length) {
            gc();
        }

        int pos = mSize;
        if (pos >= mKeys.length) {
            int n = Helper.idealIntArraySize(pos + 1);

            long[] nkeys = new long[n];
            long[] nvalues = new long[n];

            System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
            System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

            mKeys = nkeys;
            mValues = nvalues;
        }

        mKeys[pos] = key;
        mValues[pos] = value;
        mSize = pos + 1;
        return pos;
    }

    public int binarySearch(long key) {
        return binarySearch(mKeys, 0, mSize, key);
    }

    public static int binarySearch(long[] a, int start, int len, long key) {
        int high = start + len, low = start - 1, guess;
        while (high - low > 1) {
            guess = (high + low) / 2;

            if (a[guess] < key)
                low = guess;
            else
                high = guess;
        }

        if (high == start + len)
            return ~(start + len);
        else if (a[high] == key)
            return high;
        else
            return ~high;
    }

    private void checkIntegrity() {
        for (int i = 1; i < mSize; i++) {
            if (mKeys[i] <= mKeys[i - 1]) {
                for (int j = 0; j < mSize; j++) {
                    System.err.println("FAIL " + j + ": " + mKeys[j] + " -> " + mValues[j]);
                }

                throw new RuntimeException();
            }
        }
    }
    private long[] mKeys;
    private long[] mValues;
    private int mSize;
}
