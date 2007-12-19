package clus.main;

import clus.util.ClusException;
import clus.data.type.*;

public interface ClusSchemaInitializer {

	public void initSchema(ClusSchema schema) throws ClusException;

}
