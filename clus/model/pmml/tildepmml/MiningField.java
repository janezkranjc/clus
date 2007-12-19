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

package clus.model.pmml.tildepmml;
import java.io.*;

public class MiningField{

private String name;
private String usageType; // predicted or active or empty

	public MiningField(String $name, String $usageType) {
	
	name=$name;
	usageType=$usageType;
	
	}
	
	public MiningField(String $name) {
	
	name=$name;
	usageType="";
	
	}
	
	public String getName() {
	
	return name;
	
	}
	
	public String getUsageType() {
	
	return usageType;
	
	}
	
	public void print(PrintWriter outStream) {
	
	if (usageType!="") outStream.write("<MiningField name=\""+name+"\" usageType=\""+usageType+"\"></MiningField>\n");
	else outStream.write("<MiningField name=\""+name+"\"></MiningField>\n");
		
	}
	
	
	
}
