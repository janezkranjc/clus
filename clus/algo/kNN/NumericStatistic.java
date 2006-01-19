package clus.algo.kNN;

/**
 * This class stores some useful statistics for a Numeric Attribute
 * of certain data.
 */
public class NumericStatistic extends AttributeStatistic{

	private double $mean,$variance,$min,$max;

	public NumericStatistic(){
	}

	public double mean(){
		return $mean;
	}
	public void setMean(double m){
		$mean = m;
	}
	public double variance(){
		return $variance;
	}
	public void setVariance(double v){
		$variance = v;
	}
	public double min(){
		return $min;
	}
	public void setMin(double m){
		$min = m;
	}
	public double max(){
		return $max;
	}
	public void setMax(double m){
		$max = m;
	}
}