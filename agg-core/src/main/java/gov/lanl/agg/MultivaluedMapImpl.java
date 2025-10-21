package gov.lanl.agg;

  import java.io.Serializable;
  import java.util.ArrayList;
  import java.util.Collection;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;   
  import javax.ws.rs.core.MultivaluedMap;
  
  
  
   
  @SuppressWarnings("serial")
  public class MultivaluedMapImpl<K, V> extends HashMap<K, List<V>> implements MultivaluedMap<K, V>
  {
     public void putSingle(K key, V value)
     {
        List<V> list = new ArrayList<V>();
        list.add(value);
        put(key, list);
     }
  
     public final void add(K key, V value)
     {
        getList(key).add(value);
     }
  
  
     public final void addMultiple(K key, Collection<V> values)
     {
        getList(key).addAll(values);
     }
  
     public V getFirst(K key)
     {
        List<V> list = get(key);
        return list == null ? null : list.get(0);
     }
  
     public final List<V> getList(K key)
     {
        List<V> list = get(key);
        if (list == null)
           put(key, list = new ArrayList<V>());
        return list;
     }
     
     public void addAll(MultivaluedMapImpl<K, V> other)
     {
        for (Map.Entry<K, List<V>> entry : other.entrySet())
        {
           getList(entry.getKey()).addAll(entry.getValue());
        }
     }
/*
	@Override
	public void addAll(K key, V... newValues) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAll(K key, List<V> valueList) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFirst(K key, V value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> otherMap) {
		// TODO Auto-generated method stub
		return false;
	}
	*/
 }
  
 
