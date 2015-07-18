package org.sagebionetworks.warehouse.workers.bucket;

import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.aws.utils.s3.BucketDao;
import org.sagebionetworks.warehouse.workers.BucketDaoProvider;
import org.sagebionetworks.warehouse.workers.db.FileManager;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.progress.ProgressingRunner;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.google.inject.Inject;

/**
 * This worker stack will scan all buckets to find files and folders that have
 * not been discovered by the real time process.  This worker's primary task is to back-fill
 * all S3 objects that existed before the stack start.
 *
 */
public class BucketScanningWorker implements ProgressingRunner<Void> {
	
	BucketDaoProvider bucketDaoProvider;
	AmazonSQSClient awsSQSClient;
	List<BucketInfo> bucketList;
	FileManager fileManager;
	
	@Inject
	public BucketScanningWorker(BucketDaoProvider bucketDaoProvider,
			AmazonSQSClient awsSQSClient, BucketInfoList toCollate, FileManager fileManager) {
		super();
		this.bucketDaoProvider = bucketDaoProvider;
		this.awsSQSClient = awsSQSClient;
		this.bucketList = toCollate.getBucketList();
		this.fileManager = fileManager;
	}

	@Override
	public void run(ProgressCallback<Void> progressCallback) throws Exception {
		// Scan each bucket looking for files to process
		for(BucketInfo info: bucketList){
			// Helper to scan the files
			BucketDao bucketDao = bucketDaoProvider.createBucketDao(info.getBucketName());
			String nullPrefix = null;
			Iterator<S3ObjectSummary> objectStream = bucketDao.summaryIterator(nullPrefix);
			// The manager will deal with this stream.
			this.fileManager.addS3Objects(objectStream, progressCallback);
		}
	}

}