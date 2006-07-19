package clus.main;

public class Global {
	public static int itemsetcpt;
	public static int greedytreecpt;
	public static int alltreecpt;
     
	public static int get_itemsetcpt() {
		return itemsetcpt;
	}
	
	public static void set_itemsetcpt(int i) {
		//System.out.println("setting itemsetcpt :"+i);
		itemsetcpt=i;
	}
	
	public static void inc_itemsetcpt(String text) {
		int i = get_itemsetcpt();
		//System.out.println(text+" before increment :"+i);
		i++;
		set_itemsetcpt(i);
		//System.out.println(text+" after increment :"+get_itemsetcpt());
	}
	
	public static int get_greedytreecpt() {
		return greedytreecpt;
	}
	
	public static void set_greedytreecpt(int i) {
		//System.out.println("setting itemsetcpt :"+i);
		greedytreecpt=i;
	}
	
	public static void inc_greedytreecpt(String text) {
		int i = get_greedytreecpt();
		//System.out.println(text+" before increment :"+i);
		i++;
		set_greedytreecpt(i);
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
