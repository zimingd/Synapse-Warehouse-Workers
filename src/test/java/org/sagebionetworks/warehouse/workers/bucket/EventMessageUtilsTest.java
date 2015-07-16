package org.sagebionetworks.warehouse.workers.bucket;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.warehouse.workers.utils.ClasspathUtils;

import com.amazonaws.services.s3.model.S3ObjectSummary;

public class EventMessageUtilsTest {
	
	private String sampleEventJson;
	
	@Before
	public void before(){
		sampleEventJson = ClasspathUtils.loadStringFromClassPath("SampleS3Event.json");
	}
	
	@Test
	public void testParseEventJson(){
		List<S3ObjectSummary> list = EventMessageUtils.parseEventJson(sampleEventJson);
		assertNotNull(list);
		assertEquals(2, list.size());
		S3ObjectSummary summary = list.get(0);
		assertNotNull(summary);
		assertEquals("devhill.access.record.sagebase.org", summary.getBucketName());
		assertEquals("AttributeCounts.csv", summary.getKey());
		assertEquals("7607be6ca6b895634d915e89177e1376", summary.getETag());
		Date expectedDate = ISODateTimeFormat.dateTime().parseDateTime("2015-07-10T03:47:29.243Z").toDate();
		assertEquals(expectedDate, summary.getLastModified());
		assertEquals(5277L, summary.getSize());
		// The second summary has a different key.
		summary = list.get(1);
		assertNotNull(summary);
		assertEquals("devhill.access.record.sagebase.org", summary.getBucketName());
		assertEquals("keytwo.csv", summary.getKey());
	}

}
