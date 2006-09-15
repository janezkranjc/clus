package clus.main;

public class Global {
	public static int itemsetcpt;
	public static int treecpt;
     
	public static int get_itemsetcpt() {
		return itemsetcpt;
	}
	
	public static void set_itemsetcpt(int i) {
		//System.out.println("setting itemsetcpt :"+i);
		itemsetcpt=i;
	}
	
	public static void inc_itemsetcpt() {
		int i = get_itemsetcpt();
		//System.out.println(text+" before increment :"+i);
		i++;
		set_itemsetcpt(i);
		//System.out.println(text+" after increment :"+get_itemsetcpt());
	}
	
	
	public static int get_treecpt() {
		return treecpt;
	}
	
	public static void set_treecpt(int i) {
		//System.out.println("setting itemsetcpt :"+i);
		treecpt=i;
	}
	
	public static void inc_treecpt() {
		int i = get_treecpt();
		//System.out.println(text+" before increment :"+i);
		i++;
		set_treecpt(i);
		//System.out.println(text+" after increment :"+get_itemsetcpt());
	}
	 
    /**
	 * @param args
	 */
	/*
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
*/
}
