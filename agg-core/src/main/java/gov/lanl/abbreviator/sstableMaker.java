package gov.lanl.abbreviator;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.AsciiType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.DateType;
import org.apache.cassandra.db.marshal.IntegerType;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.db.marshal.CompositeType.Builder;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.io.sstable.SSTableSimpleUnsortedWriter;
import org.apache.cassandra.service.StorageService;

import static org.apache.cassandra.utils.ByteBufferUtil.bytes;

public class sstableMaker {

	//create keyspace abbreviator  with replication = {'class':'SimpleStrategy','replication_factor':1};
	//use abbreviator;
	//CREATE TABLE summary (url varchar, mdate timestamp,archive_id varchar, mcount int,PRIMARY KEY (url, mdate,archive_id));

	/* CREATE TABLE raw_data (
	  id text,
	  date text,
	  request text,
	  data1 text,
	  data2 text,
	  PRIMARY KEY (id, date, request)
	) WITH
	  bloom_filter_fp_chance=0.010000 AND
	  caching='KEYS_ONLY' AND
	  comment='' AND
	  dclocal_read_repair_chance=0.000000 AND
	  gc_grace_seconds=864000 AND
	  read_repair_chance=0.100000 AND
	  replicate_on_write='true' AND
	  compaction={'class': 'SizeTieredCompactionStrategy'} AND
	  compression={'sstable_compression': 'SnappyCompressor'};
*/	
	//create keyspace abbreviator
	//CREATE TABLE summary (url varchar, mdate timestamp,archive_id varchar, mcount int PRIMARY KEY (url, mdate,archive_id));
	
	public static void main(String[] args) {
		//try {
	        IPartitioner<?> partitioner = StorageService.getPartitioner();
	        String keyspace = "abbreviator";
	        try {
	        File directory = new File(keyspace+"/sumlinks");
	         if (!directory.exists()){
	           directory.mkdir();}
	       
	           String url="2.example.com/3";
	         
	        // List <AbstractType> compositeList = new ArrayList <AbstractType>();
	         //compositeList.add(org.apache.cassandra.db.marshal.AsciiType.instance);
	         //compositeList.add(UTF8Type.instance);        
	         //compositeUtf8Utf8Type = CompositeType.getInstance(compositeList);
	       
	         final List<AbstractType<?>> compositeTypes = new ArrayList<>();
	         compositeTypes.add(LongType.instance); //or LongType.instance ? 
	        // compositeTypes.add(DateType.instance);
	         //compositeTypes.add(AsciiType.instance);
	         compositeTypes.add(UTF8Type.instance);
	         compositeTypes.add(UTF8Type.instance);
	         //compositeTypes.add(IntegerType.instance);
	         CompositeType compositeType = CompositeType.getInstance(compositeTypes);
	         
	        SSTableSimpleUnsortedWriter simpleUnsortedWriter = new SSTableSimpleUnsortedWriter(directory,partitioner, 
			keyspace,"sumlinks",compositeType,null,32);
	        long timestamp = System.currentTimeMillis();  
	        long nanotimestamp = timestamp * 1000;
	        simpleUnsortedWriter.newRow(bytes(url));
            String _arc = "ia";
            long timestamp0 = 1372321637000L;
	        ByteBuffer date = bytes(timestamp0);
	        ByteBuffer archive_id = bytes(_arc);
            long mcount = 10L;
	        long times = System.currentTimeMillis() * 1000;
	       // compositeType.builder().add(date);
	        //compositeType.builder().add(archive_id);
	       // compositeType.builder().add(bytes("k"));
	         Builder builder =
	        	      new CompositeType.Builder(compositeType);
	        builder.add(date);
	        builder.add(archive_id);
	        builder.add(bytes("k"));
	        System.out.println("here");
	       // simpleUnsortedWriter.addColumn(compositeType.builder().add(date).add(archive_id).add(bytes("k")).build(),ByteBuffer.allocate(0), timestamp);
	       // simpleUnsortedWriter.addColumn(builder.build(),ByteBuffer.allocate(0), timestamp);
	        simpleUnsortedWriter.addColumn(builder.build(),bytes(mcount), timestamp);
	      
	        // ByteBuffer.allocate(0),
	        System.out.println("here2");
	      
	        simpleUnsortedWriter.close();
	        System.exit(0);
	        
	}
    catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	        
	        
	}
      
}
