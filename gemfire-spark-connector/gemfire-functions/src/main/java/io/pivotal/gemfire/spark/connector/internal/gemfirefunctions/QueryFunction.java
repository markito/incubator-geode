package io.pivotal.gemfire.spark.connector.internal.gemfirefunctions;

import com.gemstone.gemfire.DataSerializer;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.execute.*;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.cache.query.Query;
import com.gemstone.gemfire.internal.HeapDataOutputStream;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.execute.InternalRegionFunctionContext;
import com.gemstone.gemfire.internal.logging.LogService;
import org.apache.logging.log4j.Logger;
import java.util.Iterator;

public class QueryFunction implements Function {

  private static final long serialVersionUID = 4866641340803692882L;

  public final static String ID = "gemfire-spark-query-function";

  private final static QueryFunction instance = new QueryFunction();

  private static final Logger logger = LogService.getLogger();

  private static final int CHUNK_SIZE = 1024;

  @Override
  public String getId() {
    return ID;
  }

  public static QueryFunction getInstance() {
    return instance;
  }

  @Override
  public boolean optimizeForWrite() {
    return true;
  }

  @Override
  public boolean isHA() {
    return true;
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public void execute(FunctionContext context) {
    try {
      String[] args = (String[]) context.getArguments();
      String queryString = args[0];
      String bucketSet = args[1];
      InternalRegionFunctionContext irfc = (InternalRegionFunctionContext) context;
      LocalRegion localRegion = (LocalRegion) irfc.getDataSet();
      boolean partitioned = localRegion.getDataPolicy().withPartitioning();
      Query query = CacheFactory.getAnyInstance().getQueryService().newQuery(queryString);
      Object result = partitioned ? query.execute((InternalRegionFunctionContext) context) : query.execute();
      ResultSender<Object> sender = context.getResultSender();
      HeapDataOutputStream buf = new HeapDataOutputStream(CHUNK_SIZE, null);
      Iterator<Object> iter = ((SelectResults) result).asList().iterator();
      while (iter.hasNext()) {
        Object row = iter.next();
        DataSerializer.writeObject(row, buf);
        if (buf.size() > CHUNK_SIZE) {
          sender.sendResult(buf.toByteArray());
          logger.debug("OQL query=" + queryString + " bucket set=" + bucketSet + " sendResult(), data size=" + buf.size());
          buf.reset();
        }
      }
      sender.lastResult(buf.toByteArray());
      logger.debug("OQL query=" + queryString + " bucket set=" + bucketSet + " lastResult(), data size=" + buf.size());
      buf.reset();
    }
    catch(Exception e) {
      throw new FunctionException(e);
    }
  }

}