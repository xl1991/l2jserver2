/*
 * This file is part of l2jserver2 <l2jserver2.com>.
 *
 * l2jserver2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2jserver2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with l2jserver2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.service.database.orientdb;

import java.io.IOException;
import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.l2jserver.model.Model;
import com.l2jserver.model.Model.ObjectDesire;
import com.l2jserver.model.id.ID;
import com.l2jserver.model.id.object.allocator.IDAllocator;
import com.l2jserver.service.AbstractConfigurableService;
import com.l2jserver.service.ServiceStartException;
import com.l2jserver.service.ServiceStopException;
import com.l2jserver.service.cache.Cache;
import com.l2jserver.service.cache.CacheService;
import com.l2jserver.service.core.threading.AbstractTask;
import com.l2jserver.service.core.threading.AsyncFuture;
import com.l2jserver.service.core.threading.ScheduledAsyncFuture;
import com.l2jserver.service.core.threading.ThreadService;
import com.l2jserver.service.database.DAOResolver;
import com.l2jserver.service.database.DataAccessObject;
import com.l2jserver.service.database.DatabaseException;
import com.l2jserver.service.database.DatabaseService;
import com.l2jserver.service.database.dao.DatabaseRow;
import com.l2jserver.service.database.dao.InsertMapper;
import com.l2jserver.service.database.dao.SelectMapper;
import com.l2jserver.service.database.dao.UpdateMapper;
import com.l2jserver.util.CSVUtils;
import com.l2jserver.util.CSVUtils.CSVMapProcessor;
import com.l2jserver.util.ClassUtils;
import com.l2jserver.util.QPathUtils;
import com.l2jserver.util.factory.CollectionFactory;
import com.mysema.query.sql.ForeignKey;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.Path;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.query.nativ.ONativeSynchQuery;
import com.orientechnologies.orient.core.query.nativ.OQueryContextNative;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.tx.OTransaction;

/**
 * This is an implementation of {@link DatabaseService} that provides an layer
 * to OrientDB Document Database.
 * 
 * <h1>Internal specification</h1> <h2>The {@link Query} object</h2>
 * 
 * If you wish to implement a new {@link DataAccessObject} you should try not
 * use {@link Query} object directly because it only provides low level access
 * to the JDBC architecture. Instead, you could use an specialized class, like
 * {@link InsertQuery}, {@link SelectListQuery} or {@link SelectSingleQuery}. If
 * you do need low level access, feel free to use the {@link Query} class
 * directly.
 * 
 * <h2>The {@link SelectMapper} object</h2>
 * 
 * The {@link SelectMapper} object maps an OrientDB {@link ODocument} into an
 * Java {@link Object}.
 * 
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public abstract class AbstractOrientDatabaseService extends
		AbstractConfigurableService<OrientDatabaseConfiguration> implements
		DatabaseService {
	/**
	 * The logger
	 */
	private final Logger log = LoggerFactory
			.getLogger(AbstractOrientDatabaseService.class);

	/**
	 * The cache service
	 */
	private final CacheService cacheService;
	/**
	 * The thread service
	 */
	private final ThreadService threadService;
	/**
	 * The {@link DAOResolver} instance
	 */
	private final DAOResolver daoResolver;

	/**
	 * An cache object
	 */
	private Cache<Object, Model<?>> objectCache;
	/**
	 * Future for the auto-save task. Each object that has changed is auto saved
	 * every 1 minute.
	 */
	private ScheduledAsyncFuture autoSaveFuture;
	/**
	 * The transactioned database connection, if any.
	 */
	private final ThreadLocal<ODatabaseDocumentTx> transaction = new ThreadLocal<ODatabaseDocumentTx>();

	/**
	 * @param cacheService
	 *            the cache service
	 * @param threadService
	 *            the thread service
	 * @param daoResolver
	 *            the {@link DataAccessObject DAO} resolver
	 */
	@Inject
	public AbstractOrientDatabaseService(CacheService cacheService,
			ThreadService threadService, DAOResolver daoResolver) {
		super(OrientDatabaseConfiguration.class);
		this.cacheService = cacheService;
		this.threadService = threadService;
		this.daoResolver = daoResolver;
	}

	@Override
	protected void doStart() throws ServiceStartException {
		ODatabaseDocumentTx database;
		try {
			database = ODatabaseDocumentPool.global().acquire(config.getUrl(),
					config.getUsername(), config.getPassword());
		} catch (Exception e) {
			database = new ODatabaseDocumentTx(config.getUrl());
			if (!database.exists()) {
				database.create();
			}
		}
		// database.getStorage().addUser();
		database.getLevel1Cache().setEnable(false);
		database.getLevel2Cache().setEnable(false);

		database.close();

		// check if automatic schema update is enabled
		if (config.isAutomaticSchemaUpdateEnabled()) {
			updateSchemas();
		}

		// cache must be large enough for all world objects, to avoid
		// duplication... this would endanger non-persistent states
		objectCache = cacheService.createEternalCache("database-service",
				IDAllocator.ALLOCABLE_IDS);

		// start the auto save task
		autoSaveFuture = threadService.async(60, TimeUnit.SECONDS, 60,
				new Runnable() {
					@Override
					public void run() {
						log.debug("Auto save task started");
						int objects = 0;
						for (final Model<?> object : objectCache) {
							@SuppressWarnings("unchecked")
							final DataAccessObject<Model<?>, ?> dao = daoResolver
									.getDAO(object.getClass());
							if (dao.save(object) > 0) {
								objects++;
							}
						}
						log.info(
								"{} objects have been saved by the auto save task",
								objects);
					}
				});
	}

	@Override
	public int transaction(TransactionExecutor executor) {
		final ODatabaseDocumentTx database = ODatabaseDocumentPool.global()
				.acquire(config.getUrl(), config.getUsername(),
						config.getPassword());
		database.declareIntent(new OIntentMassiveInsert());
		transaction.set(database);
		try {
			database.begin(OTransaction.TXTYPE.OPTIMISTIC);
			int returnValue = executor.perform();
			database.commit();
			return returnValue;
		} catch (DatabaseException e) {
			database.rollback();
			throw e;
		} catch (Exception e) {
			database.rollback();
			throw new DatabaseException(e);
		} finally {
			transaction.set(null);
			database.declareIntent(null);
			database.close();
		}
	}

	@Override
	public AsyncFuture<Integer> transactionAsync(
			final TransactionExecutor executor) {
		return threadService.async(new AbstractTask<Integer>() {
			@Override
			public Integer call() throws Exception {
				return transaction(executor);
			}
		});
	}

	/**
	 * Executes an <tt>query</tt> in the database.
	 * 
	 * @param <T>
	 *            the query return type
	 * @param query
	 *            the query
	 * @return an instance of <tt>T</tt>
	 */
	public <T> T query(Query<T> query) {
		Preconditions.checkNotNull(query, "query");
		ODatabaseDocumentTx database = transaction.get();
		if (database == null)
			database = ODatabaseDocumentPool.global().acquire(config.getUrl(),
					config.getUsername(), config.getPassword());
		log.debug("Executing query {} with {}", query, database);
		try {
			return query.query(database, this);
		} finally {
			if (transaction.get() == null)
				database.close();
		}
	}

	@Override
	public <M extends Model<?>, T extends RelationalPathBase<?>> void importData(
			java.nio.file.Path path, final T entity) throws IOException {
		final ODatabaseDocumentTx database = ODatabaseDocumentPool.global()
				.acquire(config.getUrl(), config.getUsername(),
						config.getPassword());
		log.info("Importing {} to {}", path, entity);

		try {
			database.begin(OTransaction.TXTYPE.OPTIMISTIC);
			CSVUtils.parseCSV(path, new CSVMapProcessor<Object>() {
				@Override
				public Object process(Map<String, String> map) {
					final ODocument document = new ODocument(entity
							.getTableName());
					for (final Entry<String, String> entry : map.entrySet()) {
						document.field(entry.getKey(), entry.getValue());
					}
					database.save(document);
					return null;
				}
			});
			database.commit();
		} catch (IOException | RuntimeException e) {
			database.rollback();
			throw e;
		} catch (Exception e) {
			database.rollback();
			throw new DatabaseException(e);
		} finally {
			transaction.set(null);
			database.close();
		}
	}

	@Override
	public boolean updateSchema(RelationalPath<?> table) {
		final ODatabaseDocumentTx database = ODatabaseDocumentPool.global()
				.acquire(config.getUrl(), config.getUsername(),
						config.getPassword());
		log.info("Updating {} schema definition", table);

		boolean newSchema = false;
		try {
			final OSchema schemas = database.getMetadata().getSchema();
			OClass schema = schemas.getClass(table.getTableName());
			if (schema == null) {
				schema = schemas.createClass(table.getTableName());
				newSchema = true;
			}
			for (final Path<?> path : table.getColumns()) {
				final String name = path.getMetadata().getExpression()
						.toString();
				OProperty property = schema.getProperty(name);
				final OType type = getType(path.getType());
				if (property == null)
					property = schema.createProperty(path.getMetadata()
							.getExpression().toString(), type);
				if (property.getType() != type)
					property.setType(type);
				final boolean nullable = QPathUtils.isNullable(path);
				final boolean autoIncrement = QPathUtils
						.isAutoIncrementable(path);
				if (property.isNotNull() && autoIncrement)
					property.setNotNull(false);
				else if (property.isNotNull() != !nullable && !autoIncrement)
					property.setNotNull(!nullable);
				if (property.isMandatory() && autoIncrement)
					property.setMandatory(false);
				else if (property.isMandatory() != !nullable && !autoIncrement)
					property.setMandatory(!nullable);
			}
			for (final ForeignKey<?> fk : table.getForeignKeys()) {
				final String[] columns = new String[fk.getLocalColumns().size()];
				int i = 0;
				for (final Path<?> keyPath : fk.getLocalColumns()) {
					columns[i++] = keyPath.getMetadata().getExpression()
							.toString();
				}
				if (!schema.areIndexed(columns))
					schema.createIndex(StringUtils.join(columns, "-"),
							INDEX_TYPE.NOTUNIQUE, columns);
			}
			final String[] pkColumns = new String[table.getPrimaryKey()
					.getLocalColumns().size()];
			int i = 0;
			for (final Path<?> keyPath : table.getPrimaryKey()
					.getLocalColumns()) {
				pkColumns[i++] = keyPath.getMetadata().getExpression()
						.toString();
			}
			if (!schema.areIndexed(pkColumns))
				schema.createIndex("PRIMARY", INDEX_TYPE.UNIQUE, pkColumns);
			schemas.save();
		} finally {
			database.close();
		}
		return newSchema;
	}

	/**
	 * Returns the {@link OType} representing the type represented by
	 * {@link Class}
	 * 
	 * @param cls
	 *            the class
	 * @return the {@link OType}. Never null.
	 */
	private OType getType(Class<?> cls) {
		if (cls.isEnum()) {
			return OType.STRING;
		} else if (ClassUtils.isSubclass(cls, Date.class)) {
			return OType.DATETIME;
		} else {
			return OType.getTypeByClass(cls);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <I extends ID<?>, O extends Model<?>> O getCachedObject(I id) {
		Preconditions.checkNotNull(id, "id");
		log.debug("Fetching cached object {}", id);
		return (O) objectCache.get(id);
	}

	@Override
	public <I extends ID<?>, O extends Model<?>> boolean hasCachedObject(I id) {
		Preconditions.checkNotNull(id, "id");
		log.debug("Locating cached object {}", id);
		return objectCache.contains(id);
	}

	@Override
	public <I extends ID<?>, O extends Model<?>> void updateCache(I id, O value) {
		Preconditions.checkNotNull(id, "key");
		Preconditions.checkNotNull(value, "value");
		log.debug("Updating cached object {} with {}", id, value);
		objectCache.put(id, value);
	}

	@Override
	public <I extends ID<?>, O extends Model<?>> void removeCache(I id) {
		Preconditions.checkNotNull(id, "key");
		log.debug("Removing cached object {}", id);
		objectCache.remove(id);
	}

	@Override
	protected void doStop() throws ServiceStopException {
		autoSaveFuture.cancel(true);
		autoSaveFuture = null;
		cacheService.dispose(objectCache);
		objectCache = null;
		ODatabaseDocumentPool.global().close();
	}

	/**
	 * The query interface. The query will receive an connection an will be
	 * executed. The can return return a value if required.
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <R>
	 *            the return type
	 */
	public interface Query<R> {
		/**
		 * Execute the query in <tt>conn</tt>
		 * 
		 * @param database
		 *            the database instance
		 * @param service
		 *            the database service instance
		 * @return the query return value
		 */
		R query(ODatabaseDocumentTx database, DatabaseService service);
	}

	/**
	 * An base abstract query. For internal use only.
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <R>
	 *            the query return type
	 */
	private static abstract class AbstractQuery<R> implements Query<R> {
		/**
		 * Tries to update the object desire if it currently is equal to
		 * <code>expected</code>
		 * 
		 * @param object
		 *            the object to update desire
		 * @param expected
		 *            the expected desire
		 */
		protected void updateDesire(Object object, ObjectDesire expected) {
			if (object instanceof Model) {
				if (((Model<?>) object).getObjectDesire() == ObjectDesire.TRANSIENT)
					return;
				if (((Model<?>) object).getObjectDesire() != expected)
					return;
				((Model<?>) object).setObjectDesire(ObjectDesire.NONE);
			}
		}

		/**
		 * Tests if the object desire is not {@link ObjectDesire#TRANSIENT}
		 * 
		 * @param object
		 *            the object
		 * @return true if the object desire is {@link ObjectDesire#TRANSIENT}
		 */
		protected boolean testDesire(Object object) {
			if (object instanceof Model) {
				if (((Model<?>) object).getObjectDesire() == ObjectDesire.TRANSIENT)
					return true;
				return false;
			}
			return false;
		}

		/**
		 * Returns the parameter name for the given <code>path</code>
		 * 
		 * @param path
		 *            the path
		 * @return the parameter name
		 */
		protected String name(Path<?> path) {
			return path.getMetadata().getExpression().toString();
		}
	}

	/**
	 * An query implementation designed to insert new objects into the database.
	 * Optionally, it can use an pseudo primary key generator that maps the
	 * OrientDB document id as the generated ID.
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <O>
	 *            the object type used in this query
	 * @param <RI>
	 *            the raw ID type
	 * @param <I>
	 *            the ID type
	 * @param <E>
	 *            the entity type
	 */
	public static class InsertQuery<O, RI, I extends ID<? super RI>, E extends RelationalPathBase<?>>
			extends AbstractQuery<Integer> {
		/**
		 * The row mapper
		 */
		private final InsertMapper<O, RI, I, E> mapper;
		/**
		 * The query object iterator
		 */
		private final Iterator<O> iterator;
		/**
		 * The query primary key column. Only set if want auto generated IDs
		 */
		private final Path<RI> primaryKey;

		/**
		 * The query entity
		 */
		protected final E entity;

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the insert mapper
		 * @param iterator
		 *            the objects to be inserted
		 * @param primaryKey
		 *            the primary key, if any. Only required if the ID is
		 *            generated by the database engine.
		 */
		public InsertQuery(E entity, InsertMapper<O, RI, I, E> mapper,
				Path<RI> primaryKey, Iterator<O> iterator) {
			this.iterator = iterator;
			this.mapper = mapper;
			this.entity = entity;
			this.primaryKey = primaryKey;
		}

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the insert mapper
		 * @param iterator
		 *            the objects to be inserted
		 */
		public InsertQuery(E entity, InsertMapper<O, RI, I, E> mapper,
				Iterator<O> iterator) {
			this(entity, mapper, null, iterator);
		}

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the insert mapper
		 * @param objects
		 *            the objects to be inserted
		 */
		@SafeVarargs
		public InsertQuery(E entity, InsertMapper<O, RI, I, E> mapper,
				O... objects) {
			this(entity, mapper, null, Iterators.forArray(objects));
		}

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the insert mapper
		 * @param objects
		 *            the objects to be inserted
		 * @param primaryKey
		 *            the primary key, if any. Only required if the ID is
		 *            generated by the database engine.
		 */
		@SafeVarargs
		public InsertQuery(E entity, InsertMapper<O, RI, I, E> mapper,
				Path<RI> primaryKey, O... objects) {
			this(entity, mapper, primaryKey, Iterators.forArray(objects));
		}

		@Override
		@SuppressWarnings("unchecked")
		public final Integer query(ODatabaseDocumentTx database,
				DatabaseService service) {
			int rows = 0;
			final DocumentDatabaseRow row = new DocumentDatabaseRow();
			while (iterator.hasNext()) {
				final O object = iterator.next();
				if (testDesire(object))
					continue;

				row.setDocument(new ODocument(entity.getTableName()));

				mapper.insert(entity, object, row);

				row.getDocument().save();
				if (primaryKey != null && object instanceof Model) {
					final Long rawID = row.getDocument().getIdentity()
							.getClusterPosition();
					row.getDocument()
							.field(primaryKey.getMetadata().getExpression()
									.toString(), rawID.intValue());
					row.getDocument().save(); // save, again!

					final ID<? super RI> id = mapper.getPrimaryKeyMapper()
							.createID((RI) (Integer) rawID.intValue());
					((Model<ID<? super RI>>) object).setID(id);
				}
				rows++;

				updateDesire(object, ObjectDesire.INSERT);
			}
			return rows;
		}
	}

	/**
	 * An query implementation designed to update objects in the database
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <O>
	 *            the query object type
	 * @param <E>
	 *            the query entity type
	 */
	public static abstract class UpdateQuery<O, E extends RelationalPathBase<?>>
			extends AbstractQuery<Integer> {
		/**
		 * The row mapper
		 */
		private final UpdateMapper<O, E> mapper;
		/**
		 * The object iterator for this query
		 */
		private final Iterator<O> iterator;
		/**
		 * The query entity
		 */
		protected final E entity;

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the update mapper
		 * @param iterator
		 *            the objects to be inserted
		 */
		public UpdateQuery(E entity, UpdateMapper<O, E> mapper,
				Iterator<O> iterator) {
			this.iterator = iterator;
			this.mapper = mapper;
			this.entity = entity;
		}

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the update mapper
		 * @param objects
		 *            the objects to be inserted
		 */
		@SafeVarargs
		public UpdateQuery(E entity, UpdateMapper<O, E> mapper, O... objects) {
			this(entity, mapper, Iterators.forArray(objects));
		}

		@Override
		public final Integer query(ODatabaseDocumentTx database,
				DatabaseService service) {
			int rows = 0;
			final DocumentDatabaseRow row = new DocumentDatabaseRow();
			while (iterator.hasNext()) {
				final O object = iterator.next();
				if (testDesire(object))
					continue;

				List<ODocument> documents = database
						.query(new ONativeSynchQuery<OQueryContextNative>(
								database, entity.getTableName(),
								new OQueryContextNative()) {
							private static final long serialVersionUID = 1L;

							@Override
							public boolean filter(OQueryContextNative record) {
								record = query(record, object);
								if (record == null)
									return true;
								return record.go();
							};
						});
				if (documents.size() < 1)
					continue;
				row.setDocument(documents.get(0));
				mapper.update(entity, object, row);

				row.getDocument().save();
				rows++;

				updateDesire(object, ObjectDesire.UPDATE);
			}
			return rows;
		}

		/**
		 * Performs the OrientDB document filtering. If all results are wanted,
		 * <code>null</code> should be returned.
		 * 
		 * @param record
		 *            the document record
		 * @param o
		 *            the object instance
		 * @return the record instance or <code>null</code>
		 */
		protected abstract OQueryContextNative query(
				OQueryContextNative record, O o);
	}

	/**
	 * An query implementation designed for deleting objects in the database.
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <O>
	 *            the query object type
	 * @param <E>
	 *            the query entity type
	 */
	public static abstract class DeleteQuery<O, E extends RelationalPathBase<?>>
			extends AbstractQuery<Integer> {
		/**
		 * The object iterator for this query
		 */
		private final Iterator<O> iterator;
		/**
		 * This query entity
		 */
		protected final E entity;

		/**
		 * @param entity
		 *            the entity type
		 * @param iterator
		 *            the objects to be inserted
		 */
		public DeleteQuery(E entity, Iterator<O> iterator) {
			this.iterator = iterator;
			this.entity = entity;
		}

		/**
		 * @param entity
		 *            the entity type
		 * @param objects
		 *            the objects to be inserted
		 */
		@SafeVarargs
		public DeleteQuery(E entity, O... objects) {
			this(entity, Iterators.forArray(objects));
		}

		@Override
		public final Integer query(ODatabaseDocumentTx database,
				DatabaseService service) {
			int rows = 0;
			while (iterator.hasNext()) {
				final O object = iterator.next();
				if (testDesire(object))
					continue;

				List<ODocument> documents = database
						.query(new ONativeSynchQuery<OQueryContextNative>(
								database, entity.getTableName(),
								new OQueryContextNative()) {
							private static final long serialVersionUID = 1L;

							@Override
							public boolean filter(OQueryContextNative record) {
								record = query(record, object);
								if (record == null)
									return true;
								return record.go();
							};
						});
				for (final ODocument document : documents) {
					document.delete();
					rows++;
				}

				updateDesire(object, ObjectDesire.DELETE);
			}
			return rows;
		}

		/**
		 * Performs the OrientDB document filtering. If all results are wanted,
		 * <code>null</code> should be returned.
		 * 
		 * @param record
		 *            the document record
		 * @param o
		 *            the object instance
		 * @return the record instance or <code>null</code>
		 */
		protected abstract OQueryContextNative query(
				OQueryContextNative record, O o);
	}

	/**
	 * Abstract query implementation designed for selecting database objects.
	 * Internal use only.
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <R>
	 *            the query return type
	 * @param <O>
	 *            the query object type
	 * @param <RI>
	 *            the raw ID type
	 * @param <I>
	 *            the ID type
	 * @param <E>
	 *            the query entity type
	 */
	private static abstract class AbstractSelectQuery<R, O, RI, I extends ID<? super RI>, E extends RelationalPathBase<RI>>
			extends AbstractQuery<R> {
		/**
		 * This query entity type
		 */
		protected final E entity;
		/**
		 * The row mapper
		 */
		protected final SelectMapper<O, RI, I, E> mapper;

		/**
		 * @param entity
		 *            the entity type
		 * @param mapper
		 *            the object mapper
		 */
		public AbstractSelectQuery(E entity, SelectMapper<O, RI, I, E> mapper) {
			this.entity = entity;
			this.mapper = mapper;
		}

		@Override
		public final R query(ODatabaseDocumentTx database,
				DatabaseService service) {
			List<ODocument> documents = database
					.query(new ONativeSynchQuery<OQueryContextNative>(database,
							entity.getTableName(), new OQueryContextNative()) {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean filter(OQueryContextNative record) {
							record = query(record, entity);
							if (record == null)
								return true;
							return record.go();
						};
					});
			return perform(documents, service);
		}

		/**
		 * Performs the OrientDB document filtering. If all results are wanted,
		 * <code>null</code> should be returned.
		 * 
		 * @param record
		 *            the document record
		 * @param e
		 *            the query entity
		 * @return the record instance or <code>null</code>
		 */
		protected abstract OQueryContextNative query(
				OQueryContextNative record, E e);

		/**
		 * Effectively performs the query executing and mapping process
		 * 
		 * @param documents
		 *            the list of documens returned
		 * @param service
		 *            the database service
		 * @return the query result, returned directly to the user
		 */
		protected abstract R perform(List<ODocument> documents,
				DatabaseService service);

		/**
		 * Checks if the object is on the cache. Returns it if available,
		 * <code>null</code> otherwise.
		 * 
		 * @param row
		 *            the row
		 * @param database
		 *            the database service
		 * @return the object on cache, if exists.
		 */
		@SuppressWarnings("unchecked")
		protected O lookupCache(DatabaseRow row, DatabaseService database) {
			final I id = mapper.getPrimaryKeyMapper().createID(
					(RI) row.get(entity.getPrimaryKey().getLocalColumns()
							.get(0)));

			if (id != null) {
				if (database.hasCachedObject(id))
					return (O) database.getCachedObject(id);
			}
			return null;
		}

		/**
		 * Updates the cache instance
		 * 
		 * @param instance
		 *            the object instance
		 * @param database
		 *            the database service
		 */
		protected void updateCache(O instance, DatabaseService database) {
			if (instance == null)
				return;
			if (instance instanceof Model)
				database.updateCache(((Model<?>) instance).getID(),
						(Model<?>) instance);
		}
	}

	/**
	 * An query implementation designed for selecting a single object in the
	 * database
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <O>
	 *            the object type
	 * @param <RI>
	 *            the raw ID type
	 * @param <I>
	 *            the ID type
	 * @param <E>
	 *            the query entity type
	 */
	public static abstract class SelectSingleQuery<O, RI, I extends ID<? super RI>, E extends RelationalPathBase<RI>>
			extends AbstractSelectQuery<O, O, RI, I, E> {
		/**
		 * @param entity
		 *            the entity
		 * @param mapper
		 *            the mapper
		 */
		public SelectSingleQuery(E entity, SelectMapper<O, RI, I, E> mapper) {
			super(entity, mapper);
		}

		@Override
		protected final O perform(List<ODocument> documents,
				DatabaseService service) {
			final ODocument document = (!documents.isEmpty() ? documents.get(0)
					: null);
			if (document == null)
				return null;
			final DocumentDatabaseRow row = new DocumentDatabaseRow(document);
			O object = lookupCache(row, service);
			if (object == null) {
				object = mapper.select(entity, row);
				updateCache(object, service);
				updateDesire(object, ObjectDesire.INSERT);
			}
			return object;
		}
	}

	/**
	 * An query implementation designed for selecting several objects in the
	 * database
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <O>
	 *            the object type
	 * @param <RI>
	 *            the raw ID type
	 * @param <I>
	 *            the ID type
	 * @param <E>
	 *            the query entity type
	 */
	public static abstract class SelectListQuery<O, RI, I extends ID<? super RI>, E extends RelationalPathBase<RI>>
			extends AbstractSelectQuery<List<O>, O, RI, I, E> {
		/**
		 * @param entity
		 *            the entity
		 * @param mapper
		 *            the mapper
		 */
		public SelectListQuery(E entity, SelectMapper<O, RI, I, E> mapper) {
			super(entity, mapper);
		}

		@Override
		protected final List<O> perform(List<ODocument> documents,
				DatabaseService service) {
			final List<O> results = CollectionFactory.newList();
			final DocumentDatabaseRow row = new DocumentDatabaseRow();
			for (final ODocument document : documents) {
				row.setDocument(document);
				O object = lookupCache(row, service);
				if (object == null) {
					object = mapper.select(entity, row);
					updateCache(object, service);
					updateDesire(object, ObjectDesire.INSERT);
				}
				if (object != null)
					results.add(object);
			}
			return results;
		}
	}

	/**
	 * An query implementation designed to count objects in the database
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * 
	 * @param <E>
	 *            the query entity type
	 */
	public static abstract class CountQuery<E extends RelationalPathBase<?>>
			extends AbstractQuery<Integer> {
		/**
		 * The query entity
		 */
		protected final E entity;

		/**
		 * @param entity
		 *            the entity type
		 */
		public CountQuery(E entity) {
			this.entity = entity;
		}

		@Override
		public final Integer query(ODatabaseDocumentTx database,
				DatabaseService service) {
			List<ODocument> documents = database
					.query(new ONativeSynchQuery<OQueryContextNative>(database,
							entity.getTableName(), new OQueryContextNative()) {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean filter(OQueryContextNative record) {
							record = query(record, entity);
							if (record == null)
								return true;
							return record.go();
						};
					});
			return documents.size();
		}

		/**
		 * Performs the OrientDB document filtering. If all results are wanted,
		 * <code>null</code> should be returned.
		 * 
		 * @param record
		 *            the document record
		 * @param e
		 *            the entity
		 * @return the record instance or <code>null</code>
		 */
		protected abstract OQueryContextNative query(
				OQueryContextNative record, E e);
	}
}
