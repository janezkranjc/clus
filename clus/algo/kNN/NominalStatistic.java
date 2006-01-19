package clus.algo.kNN;

/**
 * This class stores some useful statistics for a Nominal Attribute
 * of certain data.
 */
public class NominalStatistic extends AttributeStatistic{

	private int $mean;

	public NominalStatistic(){
	}

	public int mean(){
		return $mean;
	}
	public void setMean(int m){
		$mean = m;
	}

}