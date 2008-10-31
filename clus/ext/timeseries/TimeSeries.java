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

package clus.ext.timeseries;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import clus.main.Settings;
import clus.util.ClusFormat;

public class TimeSeries implements Serializable{

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	private double[] values;

	public TimeSeries(String values){
		values = values.trim();
		values = values.replace("[", "");
		values = values.replace("]", "");
		//values = values.replaceAll("\\[", "");
		//values = values.replaceAll("\\]", "");
		StringTokenizer st = new StringTokenizer(values,",");
		this.values = new double[st.countTokens()];
		int i=0;
		while (st.hasMoreTokens()){
			this.values[i++]=Double.parseDouble(st.nextToken());
		}
	}

	/*
	 * [Aco]
	 * For easy printing of the series
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer a = new StringBuffer("[");
		for (int i=0; i<length()-1;i++){
			a.append(fr.format(values[i]));
			a.append(',');
		}
		if (length()>0)
			a.append(fr.format(values[length()-1]));
		a.append(']');
		return a.toString();
	}


	public TimeSeries(double[] values){
		this.values = new double[values.length];
		System.arraycopy(values, 0, this.values, 0, values.length);
	}

	public TimeSeries(int size){
		this.values = new double[size];
		for (int i=0; i<size;i++){this.values[i]=0;}
	}

	public int length(){
		if (values==null)
			return 0;
		return values.length;
	}


	public double[] getValues() {
		double[] result = new double[values.length];
		System.arraycopy(values, 0, result, 0, values.length);
		return result;
	}

	public double[] getValuesNoCopy() {
		return values;
	}

	/*
	 * [Aco]
	 * Geting a single value
	 */
	public double getValue(int index) {
		return values[index];
	}

	/*
	 * [Aco]
	 * resizing a time series, for any reason
	 * the series length must be at least one for this to work
	 */
	public void resize(int newSize, String method){
		double[] oldValues=getValues();
		int oldSize=length();
		double[] values = new double[newSize];
		int tmpOriginal;
		double w;
		double precision=0.00000001;
		if (method.compareTo("linear")==0) {
			for (int i=0;i<newSize; i++){
				tmpOriginal=(int)Math.floor( i*((float)oldSize/(float)newSize) +precision);
				w=i*((float)oldSize/(float)newSize)-tmpOriginal;
				if (Math.abs(w)<precision){
					values[i]=oldValues[tmpOriginal];
				}
				else{
					values[i]=oldValues[tmpOriginal]*(1-w)+w*oldValues[tmpOriginal+1];
				}
			}
		}
	}

	/*
	 * [Aco]
	 * rescaling a time series, for any reason
	 * the series length must be at least one for this to work
	 */
	public void rescale(double min, double max){
		double tmpMin=min();
		double tmpMax=max();
		if (tmpMax==tmpMin)	for (int i=0; i< length(); i++) values[i]=(max-min)/2;
		else for (int i=0; i< length(); i++) values[i]=((values[i]-tmpMin)/(tmpMax-tmpMin))*(max-min)+min;
	}

	/*
	 * [Aco]
	 * minimal element
	 */
	public double min(){
		double r = Double.POSITIVE_INFINITY;
		for (int i=0; i<length();i++ ){
			if (r > values[i]){r=values[i];}
		}
		return r;
	}

	/*
	 * [Aco]
	 * maximal element
	 */
	public double max(){
		double r = Double.NEGATIVE_INFINITY;
		for (int i=0; i<length();i++ ){
			if (r < values[i]){r=values[i];}
		}
		return r;

	}

	public void setValues(double[] values) {
		System.arraycopy(values, 0, this.values, 0, values.length);
	}

	/*
	 * [Aco]
	 * seting a single value
	 */
	public void setValue(int index, double value) {
		values[index]=value;
	}

}
