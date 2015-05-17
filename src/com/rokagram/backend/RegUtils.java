package com.rokagram.backend;

public class RegUtils {

	static public final int MIN_CODE_LENGTH = 4;
	// Excluding 0, O, 1, I, 5, S, and Z to avoid confusion
	// also exclude 'R' our special marker
	static private final String CODE_SYMBOLS = "ABCDEFGHKLMNPQTUVWXY";
	static private final String CODE_MARKERS = "RJ";

	static public String getToken(long id) {
		StringBuilder sbret = new StringBuilder();

		Long base = (long) CODE_SYMBOLS.length();
		int i;
		for (i = 0; i < 20; i++) {
			long val = (long) Math.pow(base, i + 1);
			if (id < val) {
				break;
			}
		}
		int tokenLen = Math.max(i, MIN_CODE_LENGTH);
		int numRandom = tokenLen - (i + 2);
		if (numRandom > 0) {
			for (int j = 0; j < numRandom; j++) {
				int pos = (int) (Math.random() * base);
				sbret.append(CODE_SYMBOLS.toCharArray()[pos]);
			}
		}

		if (numRandom >= 0) {
			sbret.append(CODE_MARKERS.charAt((int) (Math.random() * 2)));
		}
		long x = id;
		for (; i >= 0; i--) {
			long foo = (long) Math.pow(base, i);

			long pos = x / foo;
			long fooman = pos * foo;
			x -= fooman;
			sbret.append(CODE_SYMBOLS.toCharArray()[(int) pos]);
		}

		return sbret.toString();
	}

	static public Long getId(String token) {

		Long ret = null;
		long id = 0;
		String s = token;
		int markerPos = -1;
		for (char ch : CODE_MARKERS.toCharArray()) {
			markerPos = Math.max(markerPos, token.indexOf(ch));
		}
		s = token.substring(markerPos + 1, token.length());
		int slength = s.length();
		for (int i = 0; i < slength; i++) {
			int pos = CODE_SYMBOLS.indexOf(s.toCharArray()[i]);

			int base = CODE_SYMBOLS.length();
			id += pos * (Math.pow(base, slength - i - 1));
		}
		if (id == 0) {
			ret = null;
		} else {
			ret = id;
		}
		return ret;
	}

}
