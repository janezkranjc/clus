package clus.ext.hierarchical;

public class HierPrologOutput {


	public static String termToIndexList(ClassTerm term) {
		String res = String.valueOf(term.getIndex())+"]";
		ClassTerm cr = term.getCTParent();
		while (cr != null) {
			if (cr.getCTParent() != null) res = cr.getIndex() + "," + res;
			cr = cr.getCTParent();
		}
		return "["+res;
	}

}
