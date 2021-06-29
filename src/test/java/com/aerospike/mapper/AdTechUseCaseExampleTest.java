package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeOrdinal;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.virtuallist.ReturnType;
import com.aerospike.mapper.tools.virtuallist.VirtualList;
import org.junit.jupiter.api.Test;

public class AdTechUseCaseExampleTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "user")
	public static class User {
		@AerospikeKey
		private final String id;
		private final Date created;
		@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
		private final List<Segment> segments;
		
		public User(@ParamFrom("id") String id, @ParamFrom("created") Date created, @ParamFrom("segments") List<Segment> segments) {
			super();
			this.id = id;
			this.created = created;
			this.segments = segments;
		}
		public String getId() {
			return id;
		}
		public Date getCreated() {
			return created;
		}
		public List<Segment> getSegments() {
			return segments;
		}
	}
	
	@AerospikeRecord
	public static class Segment {
		@AerospikeKey
		private String name;
		@AerospikeOrdinal
		private Date lastSeen;
		@AerospikeBin(name = "partId")
		private long partnerId;
		private long flags;
		
		public Segment(@ParamFrom("name") String name, @ParamFrom("lastSeen") Date lastSeen, @ParamFrom("partId") long partnerId, @ParamFrom("flags") long flags) {
			super();
			this.name = name;
			this.lastSeen = lastSeen;
			this.partnerId = partnerId;
			this.flags = flags;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Date getLastSeen() {
			return lastSeen;
		}
		public void setLastSeen(Date lastSeen) {
			this.lastSeen = lastSeen;
		}
		public long getPartnerId() {
			return partnerId;
		}
		public void setPartnerId(long partnerId) {
			this.partnerId = partnerId;
		}
		public long getFlags() {
			return flags;
		}
		public void setFlags(long flags) {
			this.flags = flags;
		}
		
		@Override
		public String toString() {
			return String.format("{name=%s, lastSeen=%s, partnerId=%d, flags=%d}\n", name, lastSeen.toString(), partnerId, flags);
		}
	}
	
	@Test
	public void adTechUseCase() {
		Policy readPolicy = new Policy();
		readPolicy.socketTimeout = 30;
		readPolicy.maxRetries = 1;
		readPolicy.totalTimeout = 80;
		readPolicy.replica = Replica.SEQUENCE;
		
		AeroMapper mapper = new AeroMapper.Builder(client)
					.withReadPolicy(readPolicy).forAll()
				.build();
		
		User user = new User("User1", new Date(), new ArrayList<>());
		mapper.save(user);
		
		// Create a virtual list pointing to the users' segments
		VirtualList<Segment> segments = mapper.asBackedList(user, "segments", Segment.class);
		
		// Allow the date to be manipulated
		long timeInMs = new Date().getTime();

		Date cutoff = new Date(timeInMs - TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS));

		// Insert several segments into the list and remove any older ones
		segments.beginMultiOperation()
			.append(new Segment("Sports", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS)), 1234, 1L))
			.append(new Segment("Computers", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(36, TimeUnit.HOURS)), 9999, 2L))
			.append(new Segment("Elecronics", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(80, TimeUnit.HOURS)), 858, 3L))
			.append(new Segment("Fishing", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(96, TimeUnit.HOURS)), 888, 4L))
			.append(new Segment("Elecronics", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(80, TimeUnit.HOURS)), 858, 5L))
			.append(new Segment("Drinking", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(120, TimeUnit.HOURS)), 222,6L))
			.append(new Segment("Sleeping", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(160, TimeUnit.HOURS)), 111, 7L))
			.append(new Segment("Eating", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(170, TimeUnit.HOURS)), 858, 8L))
			.append(new Segment("Hiking", new Date(timeInMs - TimeUnit.MILLISECONDS.convert(2400, TimeUnit.HOURS)), 77777, 9L))
			.removeByValueRange(null, new Date[] {cutoff})
		.end();
		
		// Now, get any segments in the last 7 days
		Date readCutoff = new Date(timeInMs - TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS));
//		List<Segment> segmentList = (List<Segment>) segments.beginMulti()
//			.getByValueRange(readCutoff, null)
//			.end();
		List<Segment> segmentList = segments.getByValueRange(new Date[] {readCutoff}, null, ReturnType.ELEMENTS);
		System.out.println(segmentList);
	}
}
