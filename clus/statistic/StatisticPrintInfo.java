package clus.statistic;

public class StatisticPrintInfo {

	public boolean SHOW_EXAMPLE_COUNT = true;
	
	public boolean SHOW_DISTRIBUTION = false;
	
	public boolean SHOW_INDEX = false;
	
	public static StatisticPrintInfo m_Instance = new StatisticPrintInfo();
	
	public static StatisticPrintInfo getInstance() {
		return m_Instance;
	}
}
