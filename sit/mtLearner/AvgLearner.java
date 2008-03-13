package sit.mtLearner;


import sit.TargetSet;
import jeans.util.IntervalCollection;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.data.type.NumericAttrType;
import clus.main.Settings;
import clus.selection.XValMainSelection;
import clus.selection.XValRandomSelection;
import clus.selection.XValSelection;
import clus.util.ClusException;

/***
 * 
 * 
 * @author beau
 * Returns as prediction the average of all training instances
 *
 * 
 *  */


public class AvgLearner implements MTLearner{

	
	protected RowData m_Data;
	protected RowData m_Test = null;
	protected Settings m_Sett;
	protected XValMainSelection m_XValSel;
	
	/**************************
	 * Interface functions
	 **************************/
	 
	/**
	 * @see MTLearner
	 */ 
	public void init(RowData data, Settings sett) {
		this.m_Data = data;
		this.m_Sett = sett;
			
	}
	/**
	 * @see MTLearner
	 */
	public RowData[] LearnModel(TargetSet targets) throws Exception{
		if(m_Test == null){
			throw new Exception();
		}
		return LearnModel(targets, this.m_Data, this.m_Test);
	}
	/**
	 * @see MTLearner
	 */
	public void setData(RowData data) {
		this.m_Data = data;
	}
	/**
	 * @see MTLearner
	 */
	public void initXVal(int nrFolds) {
		try {
			m_XValSel = new XValRandomSelection(m_Data.getNbRows(),nrFolds);
		} catch (ClusException e) {
			e.printStackTrace();
		}
		
		
	}
	/**
	 * @see MTLearner
	 */
	public int initLOOXVal() {
		try {
			m_XValSel = new XValRandomSelection(m_Data.getNbRows(),m_Data.getNbRows());
		} catch (ClusException e) {
			e.printStackTrace();
		}
		
		return  m_Data.getNbRows();
		
	}
	/**
	 * @see MTLearner
	 */
	public RowData[] LearnModel(TargetSet targets, int foldNr) {
		XValSelection msel = new XValSelection(m_XValSel, foldNr);
		RowData train = (RowData) m_Data.cloneData();
		RowData test = (RowData) train.select(msel);
		return LearnModel(targets, train, test);
	}
	/**
	 * Sets the testdata to test
	 * @param the testdata
	 */
	public void setTestData(RowData test) {
		this.m_Test = test;		
	}

	
	/* ********************************
	 * Private implementation functions
	 **********************************/
	
	//the actual LearnModel function
	private RowData[] LearnModel(TargetSet targets, RowData train, RowData test){
		ClusSchema schema = m_Data.getSchema();
		//schema.getNbNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NumericAttrType[] num = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		
		
		DataTuple result = new DataTuple(schema);//the results have the same schema, non-targets may be null
		
		for(int i=0;i<train.getNbRows();i++){
			
			DataTuple tuple = train.getTuple(i);
			
			for(int j=0;j<num.length;j++){
				double d = num[j].getNumeric(tuple);
				double temp = num[j].getNumeric(result) + d;
				num[j].setNumeric(result, temp);
				
				
			}
		}
		for(int j=0;j<num.length;j++){
			double temp = num[j].getNumeric(result);
			num[j].setNumeric(result, temp/train.getNbRows());
			
			
		}
		
		RowData predictions = new RowData(schema,test.getNbRows());
		for(int i=0;i<test.getNbRows();i++){
			predictions.setTuple(result,i);
		}
		
		RowData[] final_result ={test,predictions};
		return final_result;
	}
	
}
