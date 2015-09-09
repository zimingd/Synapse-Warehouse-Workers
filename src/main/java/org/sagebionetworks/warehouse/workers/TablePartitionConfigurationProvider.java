package org.sagebionetworks.warehouse.workers;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStack;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStackConfiguration;

import com.google.inject.Inject;

public class TablePartitionConfigurationProvider implements WorkerStackConfigurationProvider{

	final WorkerStackConfiguration config;

	@Inject
	public TablePartitionConfigurationProvider(CountingSemaphore semaphore, TablePartitionWorker worker) {
		super();
		SemaphoreGatedWorkerStackConfiguration config = new SemaphoreGatedWorkerStackConfiguration();
		config.setProgressingRunner(worker);
		config.setSemaphoreLockKey(SemaphoreKey.TABLE_PARTITION_WORKER.name());
		config.setSemaphoreLockTimeoutSec(30);
		config.setSemaphoreMaxLockCount(1);
		Runnable mainRunner = new SemaphoreGatedWorkerStack(semaphore, config);
		this.config = new WorkerStackConfiguration();
		this.config.setRunner(mainRunner);
		this.config.setStartDelayMs(1013);
		// run once per 4 hour.
		this.config.setPeriodMS(4*60*60*1000);
		this.config.setWorkerName(TablePartitionWorker.class.getName());
	}

	@Override
	public WorkerStackConfiguration getWorkerConfiguration() {
		return config;
	}

}
