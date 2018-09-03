/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.s3a.s3guard.DynamoDBClientFactory;
import org.apache.hadoop.fs.s3a.s3guard.DynamoDBLocalClientFactory;
import org.apache.hadoop.fs.s3a.s3guard.S3Guard;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.internal.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.List;

import static org.apache.hadoop.fs.contract.ContractTestUtils.skip;
import static org.apache.hadoop.fs.s3a.S3ATestConstants.*;
import static org.apache.hadoop.fs.s3a.Constants.*;
import static org.apache.hadoop.fs.s3a.S3AUtils.propagateBucketOptions;
import static org.junit.Assert.*;

/**
 * Utilities for the S3A tests.
 */
public final class S3ATestUtils {
  private static final Logger LOG = LoggerFactory.getLogger(
      S3ATestUtils.class);

  /**
   * Value to set a system property to (in maven) to declare that
   * a property has been unset.
   */
  public static final String UNSET_PROPERTY = "unset";

  /**
   * Get S3A FS name.
   * @param conf configuration.
   * @return S3A fs name.
   */
  public static String getFsName(Configuration conf) {
    return conf.getTrimmed(TEST_FS_S3A_NAME, "");
  }

  /**
   * Create the test filesystem.
   *
   * If the test.fs.s3a.name property is not set, this will
   * trigger a JUnit failure.
   *
   * Multipart purging is enabled.
   * @param conf configuration
   * @return the FS
   * @throws IOException IO Problems
   * @throws AssumptionViolatedException if the FS is not named
   */
  public static S3AFileSystem createTestFileSystem(Configuration conf)
      throws IOException {
    return createTestFileSystem(conf, false);
  }

  /**
   * Create the test filesystem with or without multipart purging
   *
   * If the test.fs.s3a.name property is not set, this will
   * trigger a JUnit failure.
   * @param conf configuration
   * @param purge flag to enable Multipart purging
   * @return the FS
   * @throws IOException IO Problems
   * @throws AssumptionViolatedException if the FS is not named
   */
  public static S3AFileSystem createTestFileSystem(Configuration conf,
      boolean purge)
      throws IOException {

    String fsname = conf.getTrimmed(TEST_FS_S3A_NAME, "");

    boolean liveTest = !StringUtils.isEmpty(fsname);
    URI testURI = null;
    if (liveTest) {
      testURI = URI.create(fsname);
      liveTest = testURI.getScheme().equals(Constants.FS_S3A);
    }
    if (!liveTest) {
      // This doesn't work with our JUnit 3 style test cases, so instead we'll
      // make this whole class not run by default
      throw new AssumptionViolatedException(
          "No test filesystem in " + TEST_FS_S3A_NAME);
    }
    // patch in S3Guard options
    maybeEnableS3Guard(conf);
    S3AFileSystem fs1 = new S3AFileSystem();
    //enable purging in tests
    if (purge) {
      conf.setBoolean(PURGE_EXISTING_MULTIPART, true);
      // but a long delay so that parallel multipart tests don't
      // suddenly start timing out
      conf.setInt(PURGE_EXISTING_MULTIPART_AGE, 30 * 60);
    }
    fs1.initialize(testURI, conf);
    return fs1;
  }

  /**
   * Create a file context for tests.
   *
   * If the test.fs.s3a.name property is not set, this will
   * trigger a JUnit failure.
   *
   * Multipart purging is enabled.
   * @param conf configuration
   * @return the FS
   * @throws IOException IO Problems
   * @throws AssumptionViolatedException if the FS is not named
   */
  public static FileContext createTestFileContext(Configuration conf)
      throws IOException {
    String fsname = conf.getTrimmed(TEST_FS_S3A_NAME, "");

    boolean liveTest = !StringUtils.isEmpty(fsname);
    URI testURI = null;
    if (liveTest) {
      testURI = URI.create(fsname);
      liveTest = testURI.getScheme().equals(Constants.FS_S3A);
    }
    if (!liveTest) {
      // This doesn't work with our JUnit 3 style test cases, so instead we'll
      // make this whole class not run by default
      throw new AssumptionViolatedException("No test filesystem in "
          + TEST_FS_S3A_NAME);
    }
    // patch in S3Guard options
    maybeEnableS3Guard(conf);
    FileContext fc = FileContext.getFileContext(testURI, conf);
    return fc;
  }

  /**
   * Repeatedly attempt a callback until timeout or a {@link FailFastException}
   * is raised. This is modeled on ScalaTests {@code eventually(Closure)} code.
   * @param timeout timeout
   * @param callback callback to invoke
   * @throws FailFastException any fast-failure
   * @throws Exception the exception which caused the iterator to fail
   */
  public static void eventually(int timeout, Callable<Void> callback)
      throws Exception {
    Exception lastException;
    long endtime = System.currentTimeMillis() + timeout;
    do {
      try {
        callback.call();
        return;
      } catch (InterruptedException | FailFastException e) {
        throw e;
      } catch (Exception e) {
        lastException = e;
      }
      Thread.sleep(500);
    } while (endtime > System.currentTimeMillis());
    throw lastException;
  }

  /**
   * Get a long test property.
   * <ol>
   *   <li>Look up configuration value (which can pick up core-default.xml),
   *       using {@code defVal} as the default value (if conf != null).
   *   </li>
   *   <li>Fetch the system property.</li>
   *   <li>If the system property is not empty or "(unset)":
   *   it overrides the conf value.
   *   </li>
   * </ol>
   * This puts the build properties in charge of everything. It's not a
   * perfect design; having maven set properties based on a file, as ant let
   * you do, is better for customization.
   *
   * As to why there's a special (unset) value, see
   * {@link http://stackoverflow.com/questions/7773134/null-versus-empty-arguments-in-maven}
   * @param conf config: may be null
   * @param key key to look up
   * @param defVal default value
   * @return the evaluated test property.
   */
  public static long getTestPropertyLong(Configuration conf,
      String key, long defVal) {
    return Long.valueOf(
        getTestProperty(conf, key, Long.toString(defVal)));
  }
  /**
   * Get a test property value in bytes, using k, m, g, t, p, e suffixes.
   * {@link org.apache.hadoop.util.StringUtils.TraditionalBinaryPrefix#string2long(String)}
   * <ol>
   *   <li>Look up configuration value (which can pick up core-default.xml),
   *       using {@code defVal} as the default value (if conf != null).
   *   </li>
   *   <li>Fetch the system property.</li>
   *   <li>If the system property is not empty or "(unset)":
   *   it overrides the conf value.
   *   </li>
   * </ol>
   * This puts the build properties in charge of everything. It's not a
   * perfect design; having maven set properties based on a file, as ant let
   * you do, is better for customization.
   *
   * As to why there's a special (unset) value, see
   * {@link http://stackoverflow.com/questions/7773134/null-versus-empty-arguments-in-maven}
   * @param conf config: may be null
   * @param key key to look up
   * @param defVal default value
   * @return the evaluated test property.
   */
  public static long getTestPropertyBytes(Configuration conf,
      String key, String defVal) {
    return org.apache.hadoop.util.StringUtils.TraditionalBinaryPrefix
        .string2long(getTestProperty(conf, key, defVal));
  }

  /**
   * Get an integer test property; algorithm described in
   * {@link #getTestPropertyLong(Configuration, String, long)}.
   * @param key key to look up
   * @param defVal default value
   * @return the evaluated test property.
   */
  public static int getTestPropertyInt(Configuration conf,
      String key, int defVal) {
    return (int) getTestPropertyLong(conf, key, defVal);
  }

  /**
   * Get a boolean test property; algorithm described in
   * {@link #getTestPropertyLong(Configuration, String, long)}.
   * @param key key to look up
   * @param defVal default value
   * @return the evaluated test property.
   */
  public static boolean getTestPropertyBool(Configuration conf,
      String key,
      boolean defVal) {
    return Boolean.valueOf(
        getTestProperty(conf, key, Boolean.toString(defVal)));
  }

  /**
   * Get a string test property.
   * <ol>
   *   <li>Look up configuration value (which can pick up core-default.xml),
   *       using {@code defVal} as the default value (if conf != null).
   *   </li>
   *   <li>Fetch the system property.</li>
   *   <li>If the system property is not empty or "(unset)":
   *   it overrides the conf value.
   *   </li>
   * </ol>
   * This puts the build properties in charge of everything. It's not a
   * perfect design; having maven set properties based on a file, as ant let
   * you do, is better for customization.
   *
   * As to why there's a special (unset) value, see
   * @see <a href="http://stackoverflow.com/questions/7773134/null-versus-empty-arguments-in-maven">
   *   Stack Overflow</a>
   * @param conf config: may be null
   * @param key key to look up
   * @param defVal default value
   * @return the evaluated test property.
   */

  public static String getTestProperty(Configuration conf,
      String key,
      String defVal) {
    String confVal = conf != null ? conf.getTrimmed(key, defVal) : defVal;
    String propval = System.getProperty(key);
    return StringUtils.isNotEmpty(propval) && !UNSET_PROPERTY.equals(propval)
        ? propval : confVal;
  }

  /**
   * The exception to raise so as to exit fast from
   * {@link #eventually(int, Callable)}.
   */
  public static class FailFastException extends Exception {
    public FailFastException() {
    }

    public FailFastException(String message) {
      super(message);
    }

    public FailFastException(String message, Throwable cause) {
      super(message, cause);
    }

    public FailFastException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Verify the class of an exception. If it is not as expected, rethrow it.
   * Comparison is on the exact class, not subclass-of inference as
   * offered by {@code instanceof}.
   * @param clazz the expected exception class
   * @param ex the exception caught
   * @return the exception, if it is of the expected class
   * @throws Exception the exception passed in.
   */
  public static Exception verifyExceptionClass(Class clazz,
      Exception ex)
      throws Exception {
    if (!(ex.getClass().equals(clazz))) {
      throw ex;
    }
    return ex;
  }

  /**
   * Turn off FS Caching: use if a filesystem with different options from
   * the default is required.
   * @param conf configuration to patch
   */
  public static void disableFilesystemCaching(Configuration conf) {
    conf.setBoolean("fs.s3a.impl.disable.cache", true);
  }

  /**
   * Skip a test if encryption tests are disabled.
   * @param configuration configuration to probe
   */
  public static void skipIfEncryptionTestsDisabled(
      Configuration configuration) {
    if (!configuration.getBoolean(KEY_ENCRYPTION_TESTS, true)) {
      skip("Skipping encryption tests");
    }
  }

  /**
   * Create a test path, using the value of
   * {@link S3ATestConstants#TEST_UNIQUE_FORK_ID} if it is set.
   * @param defVal default value
   * @return a path
   */
  public static Path createTestPath(Path defVal) {
    String testUniqueForkId =
        System.getProperty(S3ATestConstants.TEST_UNIQUE_FORK_ID);
    return testUniqueForkId == null ? defVal :
        new Path("/" + testUniqueForkId, "test");
  }

  /**
   * Assume testing S3Guard is not enabled.
   */
  public static void assumeS3GuardState(boolean shouldBeEnabled,
      Configuration originalConf) throws URISyntaxException {
    boolean isEnabled = getTestPropertyBool(originalConf, TEST_S3GUARD_ENABLED,
        originalConf.getBoolean(TEST_S3GUARD_ENABLED, false));
    Assume.assumeThat("Unexpected S3Guard test state: shouldBeEnabled=" +
        shouldBeEnabled + " and isEnabled =" + isEnabled,
        shouldBeEnabled, Is.is(isEnabled));

    final String fsname = originalConf.getTrimmed(TEST_FS_S3A_NAME);
    Assume.assumeNotNull(fsname);
    final String bucket = new URI(fsname).getHost();
    final Configuration conf = propagateBucketOptions(originalConf, bucket);
    boolean usingNullImpl = S3GUARD_METASTORE_NULL.equals(
        conf.getTrimmed(S3_METADATA_STORE_IMPL, S3GUARD_METASTORE_NULL));
    Assume.assumeThat("Unexpected S3Guard test state: shouldBeEnabled=" +
        shouldBeEnabled + " but usingNullImpl=" + usingNullImpl,
        shouldBeEnabled, Is.is(!usingNullImpl));
  }

  /**
   * Conditionally set the S3Guard options from test properties.
   * @param conf configuration
   */
  public static void maybeEnableS3Guard(Configuration conf) {
    if (getTestPropertyBool(conf, TEST_S3GUARD_ENABLED,
        conf.getBoolean(TEST_S3GUARD_ENABLED, false))) {
      // s3guard is enabled.
      boolean authoritative = getTestPropertyBool(conf,
          TEST_S3GUARD_AUTHORITATIVE,
          conf.getBoolean(TEST_S3GUARD_AUTHORITATIVE, true));
      String impl = getTestProperty(conf, TEST_S3GUARD_IMPLEMENTATION,
          conf.get(TEST_S3GUARD_IMPLEMENTATION,
              TEST_S3GUARD_IMPLEMENTATION_LOCAL));
      String implClass = "";
      switch (impl) {
      case TEST_S3GUARD_IMPLEMENTATION_LOCAL:
        implClass = S3GUARD_METASTORE_LOCAL;
        break;
      case TEST_S3GUARD_IMPLEMENTATION_DYNAMODBLOCAL:
        conf.setClass(S3Guard.S3GUARD_DDB_CLIENT_FACTORY_IMPL,
            DynamoDBLocalClientFactory.class, DynamoDBClientFactory.class);
      case TEST_S3GUARD_IMPLEMENTATION_DYNAMO:
        implClass = S3GUARD_METASTORE_DYNAMO;
        break;
      case TEST_S3GUARD_IMPLEMENTATION_NONE:
        implClass = S3GUARD_METASTORE_NULL;
        break;
      default:
        fail("Unknown s3guard back end: \"" + impl + "\"");
      }
      LOG.debug("Enabling S3Guard, authoritative={}, implementation={}",
          authoritative, implClass);
      conf.setBoolean(METADATASTORE_AUTHORITATIVE, authoritative);
      conf.set(S3_METADATA_STORE_IMPL, implClass);
      conf.setBoolean(S3GUARD_DDB_TABLE_CREATE_KEY, true);
    }
  }

  /**
   * Is there a MetadataStore configured for s3a with authoritative enabled?
   * @param conf Configuration to test.
   * @return true iff there is a MetadataStore configured, and it is
   * configured allow authoritative results.  This can result in reducing
   * round trips to S3 service for cached results, which may affect FS/FC
   * statistics.
   */
  public static boolean isMetadataStoreAuthoritative(Configuration conf) {
    if (conf == null) {
      return Constants.DEFAULT_METADATASTORE_AUTHORITATIVE;
    }
    return conf.getBoolean(
        Constants.METADATASTORE_AUTHORITATIVE,
        Constants.DEFAULT_METADATASTORE_AUTHORITATIVE);
  }

  /**
   * Reset all metrics in a list.
   * @param metrics metrics to reset
   */
  public static void reset(S3ATestUtils.MetricDiff... metrics) {
    for (S3ATestUtils.MetricDiff metric : metrics) {
      metric.reset();
    }
  }

  /**
   * Print all metrics in a list.
   * @param log log to print the metrics to.
   * @param metrics metrics to process
   */
  public static void print(Logger log, S3ATestUtils.MetricDiff... metrics) {
    for (S3ATestUtils.MetricDiff metric : metrics) {
      log.info(metric.toString());
    }
  }

  /**
   * Print all metrics in a list, then reset them.
   * @param log log to print the metrics to.
   * @param metrics metrics to process
   */
  public static void printThenReset(Logger log,
      S3ATestUtils.MetricDiff... metrics) {
    print(log, metrics);
    reset(metrics);
  }

  /**
   * Helper class to do diffs of metrics.
   */
  public static final class MetricDiff {
    private final S3AFileSystem fs;
    private final Statistic statistic;
    private long startingValue;

    /**
     * Constructor.
     * Invokes {@link #reset()} so it is immediately capable of measuring the
     * difference in metric values.
     *
     * @param fs the filesystem to monitor
     * @param statistic the statistic to monitor.
     */
    public MetricDiff(S3AFileSystem fs, Statistic statistic) {
      this.fs = fs;
      this.statistic = statistic;
      reset();
    }

    /**
     * Reset the starting value to the current value.
     * Diffs will be against this new value.
     */
    public void reset() {
      startingValue = currentValue();
    }

    /**
     * Get the current value of the metric.
     * @return the latest value.
     */
    public long currentValue() {
      return fs.getInstrumentation().getCounterValue(statistic);
    }

    /**
     * Get the difference between the the current value and
     * {@link #startingValue}.
     * @return the difference.
     */
    public long diff() {
      return currentValue() - startingValue;
    }

    @Override
    public String toString() {
      long c = currentValue();
      final StringBuilder sb = new StringBuilder(statistic.getSymbol());
      sb.append(" starting=").append(startingValue);
      sb.append(" current=").append(c);
      sb.append(" diff=").append(c - startingValue);
      return sb.toString();
    }

    /**
     * Assert that the value of {@link #diff()} matches that expected.
     * @param message message to print; metric name is appended
     * @param expected expected value.
     */
    public void assertDiffEquals(String message, long expected) {
      Assert.assertEquals(message + ": " + statistic.getSymbol(),
          expected, diff());
    }

    /**
     * Assert that the value of {@link #diff()} matches that expected.
     * @param expected expected value.
     */
    public void assertDiffEquals(long expected) {
      assertDiffEquals("Count of " + this, expected);
    }

    /**
     * Assert that the value of {@link #diff()} matches that of another
     * instance.
     * @param that the other metric diff instance.
     */
    public void assertDiffEquals(MetricDiff that) {
      Assert.assertEquals(this.toString() + " != " + that,
          this.diff(), that.diff());
    }

    /**
     * Comparator for assertions.
     * @param that other metric diff
     * @return true if the value is {@code ==} the other's
     */
    public boolean diffEquals(MetricDiff that) {
      return this.diff() == that.diff();
    }

    /**
     * Comparator for assertions.
     * @param that other metric diff
     * @return true if the value is {@code <} the other's
     */
    public boolean diffLessThan(MetricDiff that) {
      return this.diff() < that.diff();
    }

    /**
     * Comparator for assertions.
     * @param that other metric diff
     * @return true if the value is {@code <=} the other's
     */
    public boolean diffLessThanOrEquals(MetricDiff that) {
      return this.diff() <= that.diff();
    }

    /**
     * Get the statistic.
     * @return the statistic
     */
    public Statistic getStatistic() {
      return statistic;
    }

    /**
     * Get the starting value; that set in the last {@link #reset()}.
     * @return the starting value for diffs.
     */
    public long getStartingValue() {
      return startingValue;
    }
  }

  /**
   * Asserts that {@code obj} is an instance of {@code expectedClass} using a
   * descriptive assertion message.
   * @param expectedClass class
   * @param obj object to check
   */
  public static void assertInstanceOf(Class<?> expectedClass, Object obj) {
    Assert.assertTrue(String.format("Expected instance of class %s, but is %s.",
        expectedClass, obj.getClass()),
        expectedClass.isAssignableFrom(obj.getClass()));
  }

  /**
   * Builds a comma-separated list of class names.
   * @param classes list of classes
   * @return comma-separated list of class names
   */
  public static <T extends Class<?>> String buildClassListString(
      List<T> classes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < classes.size(); ++i) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(classes.get(i).getName());
    }
    return sb.toString();
  }

  /**
   * This class should not be instantiated.
   */
  private S3ATestUtils() {
  }

  /**
   * Set a bucket specific property to a particular value.
   * If the generic key passed in has an {@code fs.s3a. prefix},
   * that's stripped off, so that when the the bucket properties are propagated
   * down to the generic values, that value gets copied down.
   * @param conf configuration to set
   * @param bucket bucket name
   * @param genericKey key; can start with "fs.s3a."
   * @param value value to set
   */
  public static void setBucketOption(Configuration conf, String bucket,
      String genericKey, String value) {
    final String baseKey = genericKey.startsWith(FS_S3A_PREFIX) ?
        genericKey.substring(FS_S3A_PREFIX.length())
        : genericKey;
    conf.set(FS_S3A_BUCKET_PREFIX + bucket + '.' + baseKey, value);
  }

  /**
   * Assert that a configuration option matches the expected value.
   * @param conf configuration
   * @param key option key
   * @param expected expected value
   */
  public static void assertOptionEquals(Configuration conf,
      String key,
      String expected) {
    assertEquals("Value of " + key, expected, conf.get(key));
  }

  /**
   * Assume that a condition is met. If not: log at WARN and
   * then throw an {@link AssumptionViolatedException}.
   * @param message
   * @param condition
   */
  public static void assume(String message, boolean condition) {
    if (!condition) {
      LOG.warn(message);
    }
    Assume.assumeTrue(message, condition);
  }

  public static void verifyFileStatus(FileStatus status, long size,
      long blockSize, long modTime) {
    verifyFileStatus(status, size, 0, modTime, 0, blockSize, null, null, null);
  }

  public static void verifyFileStatus(FileStatus status, long size,
      int replication,
      long modTime,
      long accessTime,
      long blockSize,
      String owner,
      String group,
      FsPermission permission) {
    String details = status.toString();
    assertFalse("Not a dir: " + details, status.isDirectory());
    assertEquals("Mod time: " + details, modTime, status.getModificationTime());
    assertEquals("File size: " + details, size, status.getLen());
    assertEquals("Block size: " + details, blockSize, status.getBlockSize());
    if (replication > 0) {
      assertEquals("Replication value: " + details, replication,
          status.getReplication());
    }
    if (accessTime != 0) {
      assertEquals("Access time: " + details, accessTime,
          status.getAccessTime());
    }
    if (owner != null) {
      assertEquals("Owner: " + details, owner, status.getOwner());
    }
    if (group != null) {
      assertEquals("Group: " + details, group, status.getGroup());
    }
    if (permission != null) {
      assertEquals("Permission: " + details, permission,
          status.getPermission());
    }
  }

  public static void verifyDirStatus(FileStatus status,
      int replication,
      long modTime,
      long accessTime,
      String owner,
      String group,
      FsPermission permission) {
    String details = status.toString();
    assertTrue("Is a dir: " + details, status.isDirectory());
    assertEquals("zero length: " + details, 0, status.getLen());

    assertEquals("Mod time: " + details, modTime, status.getModificationTime());
    assertEquals("Replication value: " + details, replication,
        status.getReplication());
    assertEquals("Access time: " + details, accessTime, status.getAccessTime());
    assertEquals("Owner: " + details, owner, status.getOwner());
    assertEquals("Group: " + details, group, status.getGroup());
    assertEquals("Permission: " + details, permission, status.getPermission());
  }

  /**
   * Get the statistics from a wrapped block output stream.
   * @param out output stream
   * @return the (active) stats of the write
   */
  public static S3AInstrumentation.OutputStreamStatistics
      getOutputStreamStatistics(FSDataOutputStream out) {
    S3ABlockOutputStream blockOutputStream
        = (S3ABlockOutputStream) out.getWrappedStream();
    return blockOutputStream.getStatistics();
  }
}
