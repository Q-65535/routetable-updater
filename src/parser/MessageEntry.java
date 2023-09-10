package parser;

public class MessageEntry {
	long id;
	short source;
	short buf;
	short dest;
	short[] tag_info = new short[5];
	byte[] delay_def = new byte[8];
	short delay_index;
	short delay_count;

	public boolean isPeriodic() {
		if ((tag_info[3] & 0x40) != 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isDiagnosic() {
		return false;
	}



	public static MessageEntry decode(String entry) {
		// clear all whitespaces.
		entry = entry.replaceAll("\\s", "");
		// clear all curly braces.
		entry = entry.replaceAll("\\{", "");
		entry = entry.replaceAll("}", "");
		String[] elements = entry.split(",", 0);
		if (elements.length != 19) {
			System.out.println("error: elements length is not 19!!!");
		}

		MessageEntry messageEntry = new MessageEntry();
		messageEntry.id = Long.decode(elements[0]);
		messageEntry.source = Short.decode(elements[1]);
		messageEntry.buf = Short.decode(elements[2]);
		messageEntry.dest = Short.decode(elements[3]);
		int messageEntryIndex = 4;
		for (int i = 0; i < 5; i++) {
			messageEntry.tag_info[i] = Short.decode(elements[messageEntryIndex + i]);
		}
		messageEntryIndex = 9;
		for (int i = 0; i < 8; i++) {
			messageEntry.delay_def[i] = Byte.decode(elements[messageEntryIndex + i]);
		}
		messageEntry.delay_index = Short.decode(elements[17]);
		messageEntry.delay_count = Short.decode(elements[18]);
		return messageEntry;
	}

	public static String encode(MessageEntry messageEntry) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String id = "0x" + String.format("%1$08X",messageEntry.id) + ",";
		sb.append(id);
		sb.append("    ");
		String source = "0x" + String.format("%1$02X",messageEntry.source) + ",";
		sb.append(source);
		sb.append("   ");
		String buf = "0x" + String.format("%1$02X",messageEntry.buf) + ",";
		sb.append(buf);
		sb.append("   ");
		String dest = "0x" + String.format("%1$02X",messageEntry.dest) + ",";
		sb.append(dest);
		sb.append("   ");
		sb.append("{");
		String firstInfo = "0x" + String.format("%1$02X",messageEntry.tag_info[0]);
		sb.append(firstInfo);
		for (int i = 1; i < 5; i++) {
			String info = ",0x" + String.format("%1$02X",messageEntry.tag_info[i]);
			sb.append(info);
		}
		sb.append("},");
		sb.append("  ");
		sb.append("{");
		String firstTag = "0x" + String.format("%1$02X",messageEntry.tag_info[0]);
		sb.append(firstTag);
		for (int i = 1; i < 8; i++) {
			String tag = ",0x" + String.format("%1$02X",messageEntry.delay_def[i]);
			sb.append(tag);
		}
		sb.append("},");
		sb.append("  ");
		String cycle_index = "0x" + String.format("%1$02X",messageEntry.delay_index) + ",";
		sb.append(cycle_index);
		sb.append("   ");
		String cycle_count = "0x" + String.format("%1$02X",messageEntry.delay_count);
		sb.append(cycle_count);
		sb.append("},\n");

		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MessageEntry that = (MessageEntry) o;

		return id == that.id;
	}

	@Override
	public int hashCode() {
		return (int) (id ^ (id >>> 32));
	}
}
