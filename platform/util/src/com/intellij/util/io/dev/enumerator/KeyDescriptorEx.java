// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.io.dev.enumerator;

import com.intellij.util.containers.hash.EqualityPolicy;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.io.UnsyncByteArrayOutputStream;
import com.intellij.util.io.dev.appendonlylog.AppendOnlyLog;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Full analog of {@link KeyDescriptor}, but with {@link ByteBuffer} instead of {@link java.io.InputStream} and
 * {@link java.io.OutputStream}
 */
@ApiStatus.Internal
public interface KeyDescriptorEx<K> extends EqualityPolicy<K> {
  /** See {@link KeyDescriptor#getHashCode(Object)} docs */
  @Override
  int getHashCode(K value);

  /** See {@link KeyDescriptor#isEqual(Object, Object)} docs */
  @Override
  boolean isEqual(K key1, K key2);


  K read(@NotNull ByteBuffer input) throws IOException;


  /**
   * Implementation should append the key to the log as single record, and return appended record id.
   * E.g.
   * <pre>
   *   int recordSize = sizeOf(key); //calculate size of K's serialized form
   *   long appendedRecordId = log.append(
   *    buffer -> {
   *      buffer.putInt(...);
   *      buffer.put(...);
   *      buffer.putShort(...);
   *      ...
   *    },
   *    recordSize
   *   );
   *   return appendedRecordId;
   * </pre>
   */
  long saveToLog(@NotNull K key,
                 @NotNull AppendOnlyLog log) throws IOException;

  /**
   * Adapts old-school {@link KeyDescriptor} to new {@link KeyDescriptorEx}.
   * <p>
   * <p/>
   * Implementation is not 100% optimal -- it does unnecessary allocations and copying -- but usually good enough to
   * start using new API (i.e. {@link com.intellij.util.io.DataEnumerator}), and see does it make any difference.
   * <p/>
   * Still, one could do better by using more 'idiomatic' API -- but it takes more effort.
   */
  static <K> KeyDescriptorEx<K> adapt(KeyDescriptor<K> oldSchoolDescriptor) {
    return new KeyDescriptorEx<K>() {
      @Override
      public int getHashCode(K value) { return oldSchoolDescriptor.getHashCode(value); }

      @Override
      public boolean isEqual(K key1, K key2) { return oldSchoolDescriptor.isEqual(key1, key2); }


      // Serialization/deserialization just bridges between ByteBuffer and ByteArrayInput/OutputStream

      //MAYBE RC: one allocation & one copy could be removed by implementing something like ByteBufferBacked[Input|Output]Stream
      //          instead of UnsyncByteArray[Input|Output]Stream

      @Override
      public K read(@NotNull ByteBuffer input) throws IOException {
        int bytesAvailable = input.remaining();
        byte[] contentAsArray = new byte[bytesAvailable];
        input.get(contentAsArray);
        return oldSchoolDescriptor.read(new DataInputStream(new UnsyncByteArrayInputStream(contentAsArray)));
      }

      @Override
      public long saveToLog(@NotNull K key,
                            @NotNull AppendOnlyLog log) throws IOException {
        UnsyncByteArrayOutputStream stream = new UnsyncByteArrayOutputStream(64);
        try (DataOutputStream os = new DataOutputStream(stream)) {
          oldSchoolDescriptor.save(os, key);
        }

        long appendedRecordId = log.append(stream.toByteArray());
        return appendedRecordId;
      }

      @Override
      public String toString() {
        return "KeyDescriptorAdapter[adapted: " + oldSchoolDescriptor + "]";
      }
    };
  }
}
