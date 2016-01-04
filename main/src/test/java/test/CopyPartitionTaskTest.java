package test;

import com.airbnb.di.common.ConfigurationKeys;
import com.airbnb.di.common.DistCpException;
import com.airbnb.di.hive.common.HiveObjectSpec;
import com.airbnb.di.hive.common.HiveMetastoreException;
import com.airbnb.di.hive.replication.ReplicationUtils;
import com.airbnb.di.hive.replication.RunInfo;
import com.airbnb.di.hive.replication.primitives.CopyPartitionTask;
import com.airbnb.di.hive.replication.configuration.DestinationObjectFactory;
import com.airbnb.di.hive.replication.configuration.ObjectConflictHandler;
import com.airbnb.di.utils.ReplicationTestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by paul_yang on 7/16/15.
 */
public class CopyPartitionTaskTest extends MockClusterTest {
    private static final Log LOG = LogFactory.getLog(
            CopyPartitionTaskTest.class);

    @Test
    public void testCopyPartition() throws IOException, HiveMetastoreException,
            DistCpException {
        // Create a partitioned table in the source
        HiveObjectSpec tableSpec = new HiveObjectSpec("test_db", "test_table");
        Table srcTable = ReplicationTestUtils.createPartitionedTable(conf,
                srcMetastore,
                tableSpec,
                TableType.MANAGED_TABLE,
                srcWarehouseRoot);

        // Create a partition in the source table
        HiveObjectSpec partitionSpec = new HiveObjectSpec("test_db",
                "test_table", "ds=1/hr=1");
        Partition srcPartition = ReplicationTestUtils.createPartition(conf,
                srcMetastore, partitionSpec);

        // Copy the partition
        Configuration testConf = new Configuration(conf);
        testConf.set(ConfigurationKeys.DISTCP_POOL, "default_pool");
        CopyPartitionTask copyPartitionTask = new CopyPartitionTask(testConf,
                new DestinationObjectFactory(),
                new ObjectConflictHandler(),
                srcCluster,
                destCluster,
                partitionSpec,
                ReplicationUtils.getLocation(srcPartition),
                null,
                directoryCopier);
        RunInfo status = copyPartitionTask.runTask();

        // Verify that the partition got copied
        assertEquals(RunInfo.RunStatus.SUCCESSFUL, status.getRunStatus());
        assertEquals(9, status.getBytesCopied());
    }

    @Test
    public void testCopyPartitionView() throws IOException,
            HiveMetastoreException, DistCpException {
        // Create a partitioned table in the source
        HiveObjectSpec tableSpec = new HiveObjectSpec("test_db",
                "test_table_view");
        ReplicationTestUtils.createPartitionedTable(conf,
                srcMetastore,
                tableSpec,
                TableType.VIRTUAL_VIEW,
                srcWarehouseRoot);

        // Create a partition in the source table
        HiveObjectSpec partitionSpec = new HiveObjectSpec("test_db",
                "test_table_view", "ds=1/hr=1");
        Partition srcPartition = ReplicationTestUtils.createPartition(conf,
                srcMetastore, partitionSpec);

        // Copy the partition
        Configuration testConf = new Configuration(conf);
        testConf.set(ConfigurationKeys.DISTCP_POOL, "default_pool");
        CopyPartitionTask copyPartitionTask = new CopyPartitionTask(conf,
                new DestinationObjectFactory(),
                new ObjectConflictHandler(),
                srcCluster,
                destCluster,
                partitionSpec,
                ReplicationUtils.getLocation(srcPartition),
                null,
                directoryCopier);
        RunInfo status = copyPartitionTask.runTask();

        // Verify that the partition got copied
        assertEquals(RunInfo.RunStatus.SUCCESSFUL, status.getRunStatus());
        assertEquals(0, status.getBytesCopied());
    }
}