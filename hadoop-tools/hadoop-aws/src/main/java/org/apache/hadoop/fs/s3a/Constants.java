/**
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

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

/**
 * All the constants used with the {@link S3AFileSystem}.
 *
 * Some of the strings are marked as {@code Unstable}. This means
 * that they may be unsupported in future; at which point they will be marked
 * as deprecated and simply ignored.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public final class Constants {

  private Constants() {
  }

  /** The minimum multipart size which S3 supports. */
  public static final int MULTIPART_MIN_SIZE = 5 * 1024 * 1024;

  // s3 access key
  public static final String ACCESS_KEY = "fs.s3a.access.key";

  // s3 secret key
  public static final String SECRET_KEY = "fs.s3a.secret.key";

  // aws credentials provider
  public static final String AWS_CREDENTIALS_PROVIDER =
      "fs.s3a.aws.credentials.provider";

  /**
   * Extra set of security credentials which will be prepended to that
   * set in {@code "hadoop.security.credential.provider.path"}.
   * This extra option allows for per-bucket overrides.
   */
  public static final String S3A_SECURITY_CREDENTIAL_PROVIDER_PATH =
      "fs.s3a.security.credential.provider.path";

  // session token for when using TemporaryAWSCredentialsProvider
  public static final String SESSION_TOKEN = "fs.s3a.session.token";

  // number of simultaneous connections to s3
  public static final String MAXIMUM_CONNECTIONS = "fs.s3a.connection.maximum";
  public static final int DEFAULT_MAXIMUM_CONNECTIONS = 15;

  // connect to s3 over ssl?
  public static final String SECURE_CONNECTIONS =
      "fs.s3a.connection.ssl.enabled";
  public static final boolean DEFAULT_SECURE_CONNECTIONS = true;

  //use a custom endpoint?
  public static final String ENDPOINT = "fs.s3a.endpoint";

  //Enable path style access? Overrides default virtual hosting
  public static final String PATH_STYLE_ACCESS = "fs.s3a.path.style.access";

  //connect to s3 through a proxy server?
  public static final String PROXY_HOST = "fs.s3a.proxy.host";
  public static final String PROXY_PORT = "fs.s3a.proxy.port";
  public static final String PROXY_USERNAME = "fs.s3a.proxy.username";
  public static final String PROXY_PASSWORD = "fs.s3a.proxy.password";
  public static final String PROXY_DOMAIN = "fs.s3a.proxy.domain";
  public static final String PROXY_WORKSTATION = "fs.s3a.proxy.workstation";

  // number of times we should retry errors
  public static final String MAX_ERROR_RETRIES = "fs.s3a.attempts.maximum";
  public static final int DEFAULT_MAX_ERROR_RETRIES = 20;

  // seconds until we give up trying to establish a connection to s3
  public static final String ESTABLISH_TIMEOUT =
      "fs.s3a.connection.establish.timeout";
  public static final int DEFAULT_ESTABLISH_TIMEOUT = 50000;

  // seconds until we give up on a connection to s3
  public static final String SOCKET_TIMEOUT = "fs.s3a.connection.timeout";
  public static final int DEFAULT_SOCKET_TIMEOUT = 200000;

  // socket send buffer to be used in Amazon client
  public static final String SOCKET_SEND_BUFFER = "fs.s3a.socket.send.buffer";
  public static final int DEFAULT_SOCKET_SEND_BUFFER = 8 * 1024;

  // socket send buffer to be used in Amazon client
  public static final String SOCKET_RECV_BUFFER = "fs.s3a.socket.recv.buffer";
  public static final int DEFAULT_SOCKET_RECV_BUFFER = 8 * 1024;

  // number of records to get while paging through a directory listing
  public static final String MAX_PAGING_KEYS = "fs.s3a.paging.maximum";
  public static final int DEFAULT_MAX_PAGING_KEYS = 5000;

  // the maximum number of threads to allow in the pool used by TransferManager
  public static final String MAX_THREADS = "fs.s3a.threads.max";
  public static final int DEFAULT_MAX_THREADS = 10;

  // unused option: maintained for compile-time compatibility.
  // if set, a warning is logged in S3A during init
  @Deprecated
  public static final String CORE_THREADS = "fs.s3a.threads.core";

  // the time an idle thread waits before terminating
  public static final String KEEPALIVE_TIME = "fs.s3a.threads.keepalivetime";
  public static final int DEFAULT_KEEPALIVE_TIME = 60;

  // the maximum number of tasks cached if all threads are already uploading
  public static final String MAX_TOTAL_TASKS = "fs.s3a.max.total.tasks";
  public static final int DEFAULT_MAX_TOTAL_TASKS = 5;

  // size of each of or multipart pieces in bytes
  public static final String MULTIPART_SIZE = "fs.s3a.multipart.size";
  public static final long DEFAULT_MULTIPART_SIZE = 67108864; // 64M

  // minimum size in bytes before we start a multipart uploads or copy
  public static final String MIN_MULTIPART_THRESHOLD =
      "fs.s3a.multipart.threshold";
  public static final long DEFAULT_MIN_MULTIPART_THRESHOLD = 134217728; // 128M

  //enable multiobject-delete calls?
  public static final String ENABLE_MULTI_DELETE =
      "fs.s3a.multiobjectdelete.enable";

  // comma separated list of directories
  public static final String BUFFER_DIR = "fs.s3a.buffer.dir";

  // switch to the fast block-by-block upload mechanism
  public static final String FAST_UPLOAD = "fs.s3a.fast.upload";
  public static final boolean DEFAULT_FAST_UPLOAD = false;

  //initial size of memory buffer for a fast upload
  @Deprecated
  public static final String FAST_BUFFER_SIZE = "fs.s3a.fast.buffer.size";
  public static final int DEFAULT_FAST_BUFFER_SIZE = 1048576; //1MB

  /**
   * What buffer to use.
   * Default is {@link #FAST_UPLOAD_BUFFER_DISK}
   * Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final String FAST_UPLOAD_BUFFER =
      "fs.s3a.fast.upload.buffer";

  /**
   * Buffer blocks to disk: {@value}.
   * Capacity is limited to available disk space.
   */

  @InterfaceStability.Unstable
  public static final String FAST_UPLOAD_BUFFER_DISK = "disk";

  /**
   * Use an in-memory array. Fast but will run of heap rapidly: {@value}.
   */
  @InterfaceStability.Unstable
  public static final String FAST_UPLOAD_BUFFER_ARRAY = "array";

  /**
   * Use a byte buffer. May be more memory efficient than the
   * {@link #FAST_UPLOAD_BUFFER_ARRAY}: {@value}.
   */
  @InterfaceStability.Unstable
  public static final String FAST_UPLOAD_BYTEBUFFER = "bytebuffer";

  /**
   * Default buffer option: {@value}.
   */
  @InterfaceStability.Unstable
  public static final String DEFAULT_FAST_UPLOAD_BUFFER =
      FAST_UPLOAD_BUFFER_DISK;

  /**
   * Maximum Number of blocks a single output stream can have
   * active (uploading, or queued to the central FileSystem
   * instance's pool of queued operations.
   * This stops a single stream overloading the shared thread pool.
   * {@value}
   * <p>
   * Default is {@link #DEFAULT_FAST_UPLOAD_ACTIVE_BLOCKS}
   */
  @InterfaceStability.Unstable
  public static final String FAST_UPLOAD_ACTIVE_BLOCKS =
      "fs.s3a.fast.upload.active.blocks";

  /**
   * Limit of queued block upload operations before writes
   * block. Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final int DEFAULT_FAST_UPLOAD_ACTIVE_BLOCKS = 4;

  // Private | PublicRead | PublicReadWrite | AuthenticatedRead |
  // LogDeliveryWrite | BucketOwnerRead | BucketOwnerFullControl
  public static final String CANNED_ACL = "fs.s3a.acl.default";
  public static final String DEFAULT_CANNED_ACL = "";

  // should we try to purge old multipart uploads when starting up
  public static final String PURGE_EXISTING_MULTIPART =
      "fs.s3a.multipart.purge";
  public static final boolean DEFAULT_PURGE_EXISTING_MULTIPART = false;

  // purge any multipart uploads older than this number of seconds
  public static final String PURGE_EXISTING_MULTIPART_AGE =
      "fs.s3a.multipart.purge.age";
  public static final long DEFAULT_PURGE_EXISTING_MULTIPART_AGE = 86400;

  // s3 server-side encryption, see S3AEncryptionMethods for valid options
  public static final String SERVER_SIDE_ENCRYPTION_ALGORITHM =
      "fs.s3a.server-side-encryption-algorithm";

  /**
   * The standard encryption algorithm AWS supports.
   * Different implementations may support others (or none).
   * Use the S3AEncryptionMethods instead when configuring
   * which Server Side Encryption to use.
   * Value: "{@value}".
   */
  @Deprecated
  public static final String SERVER_SIDE_ENCRYPTION_AES256 =
      "AES256";

  /**
   * Used to specify which AWS KMS key to use if
   * {@link #SERVER_SIDE_ENCRYPTION_ALGORITHM} is
   * {@code SSE-KMS} (will default to aws/s3
   * master key if left blank).
   * With with {@code SSE_C}, the base-64 encoded AES 256 key.
   * May be set within a JCEKS file.
   * Value: "{@value}".
   */
  public static final String SERVER_SIDE_ENCRYPTION_KEY =
      "fs.s3a.server-side-encryption.key";

  /**
   * The original key name. Never used in ASF releases,
   * but did get into downstream products.
   */
  static final String OLD_S3A_SERVER_SIDE_ENCRYPTION_KEY
      = "fs.s3a.server-side-encryption-key";

  //override signature algorithm used for signing requests
  public static final String SIGNING_ALGORITHM = "fs.s3a.signing-algorithm";

  public static final String S3N_FOLDER_SUFFIX = "_$folder$";
  public static final String FS_S3A_BLOCK_SIZE = "fs.s3a.block.size";
  public static final String FS_S3A = "s3a";

  /** Prefix for all S3A properties: {@value}. */
  public static final String FS_S3A_PREFIX = "fs.s3a.";

  /** Prefix for S3A bucket-specific properties: {@value}. */
  public static final String FS_S3A_BUCKET_PREFIX = "fs.s3a.bucket.";

  public static final int S3A_DEFAULT_PORT = -1;

  public static final String USER_AGENT_PREFIX = "fs.s3a.user.agent.prefix";

  /** Whether or not to allow MetadataStore to be source of truth. */
  public static final String METADATASTORE_AUTHORITATIVE =
      "fs.s3a.metadatastore.authoritative";
  public static final boolean DEFAULT_METADATASTORE_AUTHORITATIVE = false;

  /** read ahead buffer size to prevent connection re-establishments. */
  public static final String READAHEAD_RANGE = "fs.s3a.readahead.range";
  public static final long DEFAULT_READAHEAD_RANGE = 64 * 1024;

  /**
   * Which input strategy to use for buffering, seeking and similar when
   * reading data.
   * Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final String INPUT_FADVISE =
      "fs.s3a.experimental.input.fadvise";

  /**
   * General input. Some seeks, some reads.
   * Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final String INPUT_FADV_NORMAL = "normal";

  /**
   * Optimized for sequential access.
   * Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final String INPUT_FADV_SEQUENTIAL = "sequential";

  /**
   * Optimized purely for random seek+read/positionedRead operations;
   * The performance of sequential IO may be reduced in exchange for
   * more efficient {@code seek()} operations.
   * Value: {@value}
   */
  @InterfaceStability.Unstable
  public static final String INPUT_FADV_RANDOM = "random";

  @InterfaceAudience.Private
  @InterfaceStability.Unstable
  public static final String S3_CLIENT_FACTORY_IMPL =
      "fs.s3a.s3.client.factory.impl";

  @InterfaceAudience.Private
  @InterfaceStability.Unstable
  public static final Class<? extends S3ClientFactory>
      DEFAULT_S3_CLIENT_FACTORY_IMPL =
          DefaultS3ClientFactory.class;

  /**
   * Maximum number of partitions in a multipart upload: {@value}.
   */
  @InterfaceAudience.Private
  public static final int MAX_MULTIPART_COUNT = 10000;

  /**
   * Classname of the S3A-specific output committer factory. This
   * is what must be declared when attempting to use
   */
  @InterfaceStability.Unstable
  public static final String S3A_OUTPUT_COMMITTER_FACTORY =
      "org.apache.hadoop.fs.s3a.commit.S3AOutputCommitterFactory";

  /* Constants. */
  public static final String S3_METADATA_STORE_IMPL =
      "fs.s3a.metadatastore.impl";

  /** Minimum period of time (in milliseconds) to keep metadata (may only be
   * applied when a prune command is manually run).
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_CLI_PRUNE_AGE =
      "fs.s3a.s3guard.cli.prune.age";

  /**
   * The region of the DynamoDB service.
   *
   * This config has no default value. If the user does not set this, the
   * S3Guard will operate table in the associated S3 bucket region.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_REGION_KEY =
      "fs.s3a.s3guard.ddb.region";

  /**
   * The DynamoDB table name to use.
   *
   * This config has no default value. If the user does not set this, the
   * S3Guard implementation will use the respective S3 bucket name.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_TABLE_NAME_KEY =
      "fs.s3a.s3guard.ddb.table";

  /**
   * Whether to create the DynamoDB table if the table does not exist.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_TABLE_CREATE_KEY =
      "fs.s3a.s3guard.ddb.table.create";

  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_TABLE_CAPACITY_READ_KEY =
      "fs.s3a.s3guard.ddb.table.capacity.read";
  public static final long S3GUARD_DDB_TABLE_CAPACITY_READ_DEFAULT = 500;
  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_TABLE_CAPACITY_WRITE_KEY =
      "fs.s3a.s3guard.ddb.table.capacity.write";
  public static final long S3GUARD_DDB_TABLE_CAPACITY_WRITE_DEFAULT = 100;

  /**
   * The maximum put or delete requests per BatchWriteItem request.
   *
   * Refer to Amazon API reference for this limit.
   */
  public static final int S3GUARD_DDB_BATCH_WRITE_REQUEST_LIMIT = 25;

  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_MAX_RETRIES =
      "fs.s3a.s3guard.ddb.max.retries";
  /**
   * Max retries on batched DynamoDB operations before giving up and
   * throwing an IOException.  Default is {@value}. See core-default.xml for
   * more detail.
   */
  public static final int S3GUARD_DDB_MAX_RETRIES_DEFAULT = 9;

  /**
   * Period of time (in milliseconds) to sleep between batches of writes.
   * Currently only applies to prune operations, as they are naturally a
   * lower priority than other operations.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_DDB_BACKGROUND_SLEEP_MSEC_KEY =
      "fs.s3a.s3guard.ddb.background.sleep";
  public static final int S3GUARD_DDB_BACKGROUND_SLEEP_MSEC_DEFAULT = 25;

  /**
   * V1 committer.
   */
  @InterfaceStability.Unstable
  public static final String S3A_OUTPUT_COMMITTER_MRV1 =
      "org.apache.hadoop.fs.s3a.commit.S3OutputCommitterMRv1";

  /**
   * The default "Null" metadata store: {@value}.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_METASTORE_NULL
      = "org.apache.hadoop.fs.s3a.s3guard.NullMetadataStore";

  /**
   * Use Local memory for the metadata: {@value}.
   * This is not coherent across processes and must be used for testing only.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_METASTORE_LOCAL
      = "org.apache.hadoop.fs.s3a.s3guard.LocalMetadataStore";

  /**
   * Use DynamoDB for the metadata: {@value}.
   */
  @InterfaceStability.Unstable
  public static final String S3GUARD_METASTORE_DYNAMO
      = "org.apache.hadoop.fs.s3a.s3guard.DynamoDBMetadataStore";

}
