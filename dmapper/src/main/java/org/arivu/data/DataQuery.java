package org.arivu.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.arivu.data.RowMapper.Identifier;
import org.arivu.datastructure.DoublyLinkedList;

/**
 * @author P
 *
 */
public class DataQuery {

	public static Identifier defaultIdentifier = new Identifier(){
		@Override
		public String getIdentity(Field field) {
			return field.getAnnotation(Column.class).name();
		}
		
	};
	
	interface DBCallback<T> {
		void onComplete(T result, Throwable err);
	}

	/**
	 * 
	 */
	private final DataSource ds;

	/**
	 * @param ds
	 */
	public DataQuery(DataSource ds) {
		super();
		this.ds = ds;
	}

	/**
	 * @param query
	 * @param parameters
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	public final Collection<Map<String, Object>> execute(final String query, final List<Parameter> parameters)
			throws IllegalAccessException, InvocationTargetException, SQLException {
		return CallableType.getByQuery(query, ds).call(query, parameters);
	}

	/**
	 * @param query
	 * @param parameters
	 * @param callback
	 * @param executor
	 */
	public final void execute(final String query, final List<Parameter> parameters,
			final DBCallback<Collection<Map<String, Object>>> callback,
			Executor executor) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					callback.onComplete(execute(query, parameters), null);
				} catch (Throwable e) {
					callback.onComplete(null, e);
				}
			}
		});
	}

	/**
	 * @param rows
	 * @param klass
	 * @param annotation
	 * @param identifier
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public <T extends Annotation, R> Collection<R> map(Collection<Map<String, Object>> rows, Class<R> klass,
			Class<T> annotation, final Identifier identifier)
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		final RowMapper<T, R> rm = new RowMapper<T, R>();
		final Collection<R> ret = new DoublyLinkedList<R>();
		for (Map<String, Object> valueObject : rows) {
			ret.add(rm.set(valueObject, annotation, true, identifier, klass));
		}
		return ret;
	}

	/**
	 * @param rows
	 * @param klass
	 * @param annotation
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public <R> Collection<R> map(Collection<Map<String, Object>> rows, Class<R> klass)
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		final RowMapper<Column, R> rm = new RowMapper<Column, R>();
		final Collection<R> ret = new DoublyLinkedList<R>();
		for (Map<String, Object> valueObject : rows) {
			ret.add(rm.set(valueObject, Column.class, true, defaultIdentifier, klass));
		}
		return ret;
	}
	
	/**
	 * @param rows
	 * @param klass
	 * @param annotation
	 * @param identifier
	 * @return
	 */
	public <T extends Annotation, R> void map(final Collection<Map<String, Object>> rows, final Class<R> klass,
			final Class<T> annotation, final Identifier identifier,
			final DBCallback<Collection<R>> callback,
			Executor executor) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					callback.onComplete(map( rows, klass, annotation, identifier), null);
				} catch (Throwable e) {
					callback.onComplete(null, e);
				}
			}
		});
	}
	
	/**
	 * @param rows
	 * @param klass
	 * @param annotation
	 * @param identifier
	 * @return
	 */
	public <R> void map(final Collection<Map<String, Object>> rows, final Class<R> klass, final DBCallback<Collection<R>> callback,
			Executor executor) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					callback.onComplete(map(rows, klass), null);
				} catch (Throwable e) {
					callback.onComplete(null, e);
				}
			}
		});
	}
}
