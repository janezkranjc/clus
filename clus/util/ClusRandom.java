package clus.util;

import clus.main.*;

import java.util.*;

public class ClusRandom {

	public static int m_Preset;
	public static boolean m_IsPreset;
	
	public final static int NB_RANDOM = 5;
	public final static int RANDOM_TEST_DIR = 0;
	public final static int RANDOM_SELECTION = 1;
	public final static int RANDOM_PARAM_TUNE = 2;
	public final static int RANDOM_CREATE_DATA = 3;
	public final static int RANDOM_ALGO_INTERNAL = 4;	
		
	public static Random[] m_Random;

	public static Random getRandom(int idx) {
		return m_Random[idx];
	}
	
	public static double nextDouble(int which) {
		return m_Random[which].nextDouble();
	}	

	public static int nextInt(int which, int max) {
		return m_Random[which].nextInt(max);
	}	
	
	public static void initialize(Settings sett) {
		m_Random = new Random[NB_RANDOM];
		if (sett.isPresetRandom()) {
			m_IsPreset = true;
			m_Preset = sett.getPresetRandom();
			for (int i = 0; i < NB_RANDOM; i++) {
				m_Random[i] = new Random(m_Preset);
			}
		} else {
			for (int i = 0; i < NB_RANDOM; i++) {
				m_Random[i] = new Random();
			}
		}
	}
	
	public static void initialize(int initial) {
		m_Random = new Random[NB_RANDOM];
		for (int i = 0; i < NB_RANDOM; i++) {
			m_Random[i] = new Random(initial);
		}
	}

	public static void reset(int rnd) {
		if (m_IsPreset) {
			m_Random[rnd] = new Random(m_Preset);
		} else {
			m_Random[rnd] = new Random(m_Preset);
		}
	}	
}
