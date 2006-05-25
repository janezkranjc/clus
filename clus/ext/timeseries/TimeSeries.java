package clus.ext.timeseries;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import clus.main.Settings;
import clus.statistic.StatisticPrintInfo;
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
		a.append(fr.format(values[length()-1]));
		a.append(']');
		return a.toString();
	}
	
	
	public TimeSeries(double[] values){
		this.values = new double[values.length];
		System.arraycopy(values, 0, this.values, 0, values.length); 
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

	/*
	 * [Aco]
	 * Seting a single value 
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
