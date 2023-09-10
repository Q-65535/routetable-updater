package parser;

import java.text.*;
import java.util.*;

public class TableVersionEntry {
    byte verNum1;
    byte verNum2;
	int year;
	int month;
	int day;
	int changeTimes;

    public void update() {
		verNum2++;
		if (verNum2 == 0) {
			verNum1++;
		}
		Format f = new SimpleDateFormat("yy.MM.dd");
		String strDate = f.format(new Date());
		// 3 elements in the array representing year, month and day saperately.
		String[] splittedDate = strDate.split("\\.");
		for (int i = 0; i < splittedDate.length; i++) {
			if (splittedDate[i].charAt(0) == '0') {
				splittedDate[i] = splittedDate[i].substring(1, splittedDate[i].length());
			}
		}

		year = Integer.decode(splittedDate[0]);
		month = Integer.decode(splittedDate[1]);
		day = Integer.decode(splittedDate[2]);
    }

	public String encode() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String verNum1Str = "0x" + String.format("%1$02X",verNum1) + ",";
		sb.append(verNum1Str);
		String verNum2Str = "0x" + String.format("%1$02X",verNum2) + ",";
		sb.append(verNum2Str);
		String yearStr = String.format("%d",year) + "u,";
		sb.append(yearStr);
		String monthStr = String.format("%d",month) + "u,";
		sb.append(monthStr);
		String dayStr = String.format("%d",day) + "u,";
		sb.append(dayStr);
		String changeTimesStr = String.format("%d",changeTimes) + "u";
		sb.append(changeTimesStr);
		sb.append("}");
        return sb.toString();
	}

	public static TableVersionEntry decode(String entry) {
		// clear all whitespaces.
		entry = entry.replaceAll("\\s", "");
		// clear all curly braces.
		entry = entry.replaceAll("\\{", "");
		entry = entry.replaceAll("}", "");
		// clear unsigned sign
		entry = entry.replaceAll("u", "");
		String[] elements = entry.split(",", 0);
		if (elements.length != 6) {
			System.out.println("error: elements length is not 6!!!");
		}

		TableVersionEntry tve = new TableVersionEntry();
		tve.verNum1 = Byte.decode(elements[0]);
		tve.verNum2 = Byte.decode(elements[1]);
		tve.year = Byte.decode(elements[2]);
		tve.month = Byte.decode(elements[3]);
		tve.day = Byte.decode(elements[4]);
		tve.changeTimes = Byte.decode(elements[5]);
        return tve;
	}
}
