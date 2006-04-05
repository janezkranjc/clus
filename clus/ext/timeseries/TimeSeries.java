package clus.ext.timeseries;

import java.util.StringTokenizer;

public class TimeSeries {
	private double[] values;
	

	public TimeSeries(String values){
		values = values.trim();
		values.replaceAll("\\[", "");
		values.replaceAll("\\]", "");
		StringTokenizer st = new StringTokenizer(values,",");
		this.values = new double[st.countTokens()];
		int i=0;
		while (st.hasMoreTokens()){
			this.values[i++]=Double.parseDouble(st.nextToken());
		}
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


	public void setValues(double[] values) {
		System.arraycopy(values, 0, this.values, 0, values.length);
	}
	
	

}
