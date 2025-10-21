package gov.lanl.agg.utils;

import java.util.Iterator;
import java.util.List;

class Robin {
private String ip;
//byte ip1[] = new byte[] { (byte)192, (byte)168, (byte)100, (byte)32 };
//InetAddress byIpAsName = InetAddress.getByName("64.69.35.190"); 
public Robin(String ip) {
this.ip = ip;
}
  
public String call() {
return ip;
}
}
public class RoundRobin {
		private Iterator<Robin> it;
		private List<Robin> list;
		  
		public RoundRobin(List<Robin> list) {
		this.list = list;
		it = list.iterator();
		}
		 
		public String next() {
		// if we get to the end, start again
		if (!it.hasNext()) {
		it = list.iterator();
		}
		Robin robin = it.next();
		 
		return robin.call();
		}
		}

