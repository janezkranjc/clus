package clus.util;

public class ClusUtil {

	public final static double SMALL = 1e-6;	
	
	public static boolean grOrEq(double a,double b) {
		/* a > b - SMALL */
	    return (b-a < SMALL);
	}	
	
	public static boolean smOrEq(double a,double b) {
		return (a-b < SMALL);
	}
	
	public static boolean eq(double a, double b){
	    return (a - b < SMALL) && (b - a < SMALL); 
	}	
}
