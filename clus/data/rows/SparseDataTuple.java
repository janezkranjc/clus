/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.data.rows;

import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.main.Settings;

import java.io.PrintWriter;
import java.util.*;

public class SparseDataTuple extends DataTuple {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected HashMap map = new HashMap();

	public SparseDataTuple(ClusSchema schema) {
		super(schema);
	}

	public void setDoubleValueSparse(double val, Integer index) {
		map.put(index, new Double(val));
	}

	public double getDoubleValueSparse(Integer index) {
		Double value = (Double)map.get(index);
		return value != null ? value.doubleValue() : 0.0;
	}
	
	public void writeTuple(PrintWriter wrt) {
		ClusSchema schema = getSchema();
		int aidx = 0;
		wrt.print("{");
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			Integer idx = (Integer)it.next();
			ClusAttrType type = schema.getAttrType(idx.intValue());
			if (!type.isDisabled()) {
				if (aidx != 0) wrt.print(",");
				wrt.print(idx+" "+type.getString(this));
				aidx++;
			}
		}
		// FIXME write non-sparse attributes?
		ClusAttrType[] type = schema.getAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
		for (int i = 0; i < type.length; i++) {
			if (!type[i].isDisabled()) {
				if (aidx != 0) wrt.print(",");
				wrt.print(type[i].getIndex()+" "+type[i].getString(this));
				aidx++;
			}
		}
		wrt.println("}");
	}	
}
