package istat.android.data.access.interfaces;

import java.util.List;
import java.util.Map;

public interface DataAccess {
	public void persist(Object obj);

	public boolean exist(Object obj);

	public <T> int delete(Class<T> clazz, Map<String, String> vars);

	public <T> List<T> find(Class<T> clazz, Map<String, String> vars);

	public <T> List<T> findById(Class<T> clazz, String id);
}
