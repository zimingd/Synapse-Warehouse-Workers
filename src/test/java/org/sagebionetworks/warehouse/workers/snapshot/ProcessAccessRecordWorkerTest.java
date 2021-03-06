package org.sagebionetworks.warehouse.workers.snapshot;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.csv.utils.ObjectCSVReader;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.warehouse.workers.collate.StreamResourceProvider;
import org.sagebionetworks.warehouse.workers.db.snapshot.ProcessedAccessRecordDao;
import org.sagebionetworks.warehouse.workers.model.SnapshotHeader;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.common.util.progress.ProgressCallback;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.model.Message;

public class ProcessAccessRecordWorkerTest {

	AmazonS3Client mockS3Client;
	ProcessedAccessRecordDao mockDao;
	ProcessAccessRecordWorker worker;
	ProgressCallback<Message> mockCallback;
	Message message;
	String messageBody;
	StreamResourceProvider mockStreamResourceProvider;
	File mockFile;
	ObjectCSVReader<AccessRecord> mockObjectCSVReader;

	@SuppressWarnings("unchecked")
	@Before
	public void before() {
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockDao = Mockito.mock(ProcessedAccessRecordDao.class);
		mockStreamResourceProvider = Mockito.mock(StreamResourceProvider.class);
		worker = new ProcessAccessRecordWorker(mockS3Client, mockDao, mockStreamResourceProvider);
		mockCallback = Mockito.mock(ProgressCallback.class);

		messageBody = "<Message>\n"
				+"  <bucket>dev.access.record.sagebase.org</bucket>\n"
				+"  <key>0000000001/node/2015-07-30/23-34-16-308-e4ccd5c9-8f61-4043-bbe2-df6578b4672f.csv.gz</key>\n"
				+"</Message>";
		message = new Message();
		message.setBody(messageBody);

		mockFile = Mockito.mock(File.class);
		mockObjectCSVReader = Mockito.mock(ObjectCSVReader.class);
		Mockito.when(mockStreamResourceProvider.createTempFile(Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_SUFFIX))).thenReturn(mockFile);
		Mockito.when(mockStreamResourceProvider.createObjectCSVReader(mockFile, AccessRecord.class, SnapshotHeader.ACCESS_RECORD_HEADERS)).thenReturn(mockObjectCSVReader);
		Mockito.when(mockDao.doesPartitionExistForTimestamp(Mockito.anyLong())).thenReturn(true);
	}

	@Test
	public void runTest() throws RecoverableMessageException, IOException {
		worker.run(mockCallback, message);
		Mockito.verify(mockStreamResourceProvider).createTempFile(Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_SUFFIX));
		Mockito.verify(mockS3Client).getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile));
		Mockito.verify(mockStreamResourceProvider).createObjectCSVReader(mockFile, AccessRecord.class, SnapshotHeader.ACCESS_RECORD_HEADERS);
		Mockito.verify(mockFile).delete();
		Mockito.verify(mockObjectCSVReader).close();
	}

	@Test (expected=RecoverableMessageException.class)
	public void invalidTimeTest() throws RecoverableMessageException, IOException {
		Mockito.when(mockDao.doesPartitionExistForTimestamp(Mockito.anyLong())).thenReturn(false);
		worker.run(mockCallback, message);
	}

	@Test
	public void deleteFileTest() throws RecoverableMessageException, IOException {
		Mockito.when(mockS3Client.getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile))).thenThrow(new AmazonClientException(""));
		try {
			worker.run(mockCallback, message);
		} catch (AmazonClientException e) {
			// expected
		}
		Mockito.verify(mockStreamResourceProvider).createTempFile(Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(ProcessAccessRecordWorker.TEMP_FILE_NAME_SUFFIX));
		Mockito.verify(mockS3Client).getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile));
		Mockito.verify(mockStreamResourceProvider, Mockito.never()).createObjectCSVReader(mockFile, AccessRecord.class, SnapshotHeader.ACCESS_RECORD_HEADERS);
		Mockito.verify(mockFile).delete();
		Mockito.verify(mockObjectCSVReader, Mockito.never()).close();
	}
}
