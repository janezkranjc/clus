    public final void evalTest(RowData data) {
	MyArray test = new MyArray();
	NodeTest t1 = makeTest(m_Schema, "calcofluor_white", "r");
	test.addElement(t1);
	NodeTest t2 = makeTest(m_Schema, "notn", "1");
	test.addElement(t2);
	ClusStatistic tstat = m_Induce.getStatManager().createStatistic();
	ClusStatistic pstat = tstat.cloneStat();
	ClusStatistic mstat = tstat.cloneStat();
	for (int i = 0; i < data.getNbRows(); i++) {
	    DataTuple tuple = data.getTuple(i);
	    int res = applyTest(tuple, test);
	    tstat.updateWeighted(tuple, i);
	    if (res == ClusNode.YES) {
		//				Object obj = tuple.getObj(0);
		//				System.out.println("Example: "+obj);
		pstat.updateWeighted(tuple, i);
	    }
	    if (res == NodeTest.UNKNOWN) mstat.updateWeighted(tuple, i);
	}
	System.out.println("Total:           "+tstat.m_SumWeight);
	System.out.println("Missing:         "+mstat.m_SumWeight);
	System.out.println("Positive:        "+pstat.m_SumWeight);
	System.out.println("Total:           "+tstat.getString());
	tstat.subtractFromThis(mstat);
	System.out.println("Total (no miss): "+tstat.getString());
	System.out.println("Positive:        "+pstat.getString());
    }

    public final int applyTest(DataTuple tuple, MyArray test) {
	for (int i = 0; i < test.size(); i++) {
	    NodeTest ti = (NodeTest)test.elementAt(i);
	    int res = ti.predictWeighted(tuple);
	    if (res == NodeTest.UNKNOWN) return NodeTest.UNKNOWN;
	    if (res == ClusNode.NO) return ClusNode.NO;
	}
	return ClusNode.YES;
    }

    public final NodeTest makeTest(ClusSchema schema, String attr, String vals) {
	NominalAttrType type = (NominalAttrType)schema.getAttrType(attr);
	SubsetTest test = new SubsetTest(type, 1);
	type.setReader(true);
	Integer mint = type.getValueIndex(vals);
	test.setValue(0, mint.intValue());
	type.setReader(false);
	return test;
    }
    
    public final void doLoad(String fname) throws IOException, ClusException {
	try {
	    ObjectLoadStream open = new ObjectLoadStream(new FileInputStream(fname));
	    ClusNode root = (ClusNode)open.readObject();
	    open.close();

	    System.out.println("Tree:");
	    root.printTree(ClusFormat.OUT_WRITER, "");
	    ClusFormat.OUT_WRITER.flush();

	    HierStatistic tr_def = (HierStatistic)root.getTotalStat();
	    ClassHierarchy hier = tr_def.getHier();

	    TargetSchema tschema = m_Induce.getStatManager().getTargetSchema();
	    hier.setType((ClassesAttrType)tschema.getType(ClassesAttrType.THIS_TYPE, 0));
	    hier.initialize();
	    m_Induce.getStatManager().setHier(hier);

	    MyClusInitializer init = new MyClusInitializer();
	    TupleIterator iter = new DiskTupleIterator(m_Sett.getTestFile(), init, getPreprocs());

	    iter.init();
	    ClusSchema mschema = iter.getSchema();
	    mschema.attachTree(root);

	    MyArray test = new MyArray();
	    NodeTest t1 = makeTest(mschema, "calcofluor_white", "r");
	    test.addElement(t1);
	    NodeTest t2 = makeTest(mschema, "notn", "1");
	    test.addElement(t2);
	    mschema.setReader(true);

	    // Put statistics in tree nodes :-)
	    SPMDStatistic ts_def = new SPMDStatistic(hier);
	    LeafTreeIterator iter1 = new LeafTreeIterator(root);
	    while (iter1.hasMoreNodes()) {
		ClusNode node = (ClusNode)iter1.getNextNode();
		DuoObject vis = new DuoObject(ts_def.cloneStat(), ts_def.cloneStat());
		node.setVisitor(vis);
	    }

	    HierMProc proc = new HierMProc();
	    MyArray procs = new MyArray();
	    procs.addElement(proc);

	    int idx = 0;
	    DataTuple tuple = iter.readTuple();
	    while (tuple != null) {

				// Execute test on data
		int res = applyTest(tuple, test);
		if (res == ClusNode.YES) {
		    Object obj = tuple.getObjVal(0);
		    System.out.println("Example: "+obj);

		    ts_def.showUpdateSpecial(tuple);
		}

				// Update defaults on testset
		ts_def.updateSpecial(tuple);
				// Update stat in node
		root.applyModelProcessors(tuple, procs);
		tuple = iter.readTuple();
		idx++;
	    }
	    iter.close();

	    LeafTreeIterator iter2 = new LeafTreeIterator(root);
	    while (iter2.hasMoreNodes()) {
		ClusNode node = (ClusNode)iter2.getNextNode();
		DuoObject obj = (DuoObject)node.getVisitor();
		SPMDStatistic s1 = (SPMDStatistic)obj.getObj1();
		SPMDStatistic s2 = (SPMDStatistic)obj.getObj2();
		ClusStatistic oldTotal = node.getTotalStat();
		node.setTotalStat(new HierSummStat((HierStatistic)oldTotal, s1, s2, ts_def, tr_def));
	    }

	    System.out.println("Tree:");
	    root.printTree(ClusFormat.OUT_WRITER, "");
	    ClusFormat.OUT_WRITER.flush();
	} catch (ClassNotFoundException e) {
	    throw new ClusException("Class not found: "+e.getMessage());
	}
    }
    
