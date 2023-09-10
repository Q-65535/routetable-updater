package parser;

import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.*;

public class TableProcessor {
	File table;
	File config;
	File newMessagesFile;

	public TableProcessor(String targetPath, String newMessagesPath) {
		table = new File(targetPath);
		newMessagesFile = new File(newMessagesPath);
	}

	public void process() throws IOException {
		// buffer for storing the changed file content.
		StringBuilder sb = new StringBuilder();
		ArrayList<MessageEntry> newMessages = decodeMessages();
		// the acutal number of entries added to the table, i.e., we don't count changed
		// entries
		int finalNewMessagesCount = newMessages.size();
		FileReader fr = new FileReader(table);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		String tempLine;
		short curBuf = 0;
		short curDelayIndex = 0;
		short curDelayCount = 0;
		boolean haveReachedIf = false;
		while ((tempLine = br.readLine()) != null) {
			line = tempLine;
			if (!isEntry(line)) {
				// reach the end of the common entrys section
				// @Problem: this if statement results in the empty line at a wrong position
				if (line.contains("#if") && haveReachedIf == false) {
					haveReachedIf = true;
					for (Iterator<MessageEntry> iter = newMessages.listIterator(); iter.hasNext();) {
						MessageEntry curNewMessage = iter.next();
						if (curNewMessage.isPeriodic()) {
							curNewMessage.delay_index = (short) (curDelayIndex + curDelayCount);
							curDelayIndex = curNewMessage.delay_index;
							curDelayCount = curNewMessage.delay_count;
						} else if (!curNewMessage.isPeriodic() && !curNewMessage.isDiagnosic()) {
							curNewMessage.buf = nextBuf(curBuf);
							curBuf = curNewMessage.buf;
						}
						iter.remove();
						sb.append(MessageEntry.encode(curNewMessage));
					}
					sb.append("\n");
				// reach the end of file (for diagnosis message)
				} else if ("}".equals(line.trim())) {
					for (Iterator<MessageEntry> iter = newMessages.listIterator(); iter.hasNext();) {
						MessageEntry curNewMessage = iter.next();
						if (!curNewMessage.isDiagnosic()) {
							throw new RuntimeException("error: this left message is not diagnosis!");
						}
						iter.remove();
						sb.append(MessageEntry.encode(curNewMessage));
					}
				}

				// @Note: readline() doesn't contain line-termination chars, so we need
				// to add "\n" to the end of each line.
				sb.append(line + "\n");
				continue;
			}
			MessageEntry curMessage = MessageEntry.decode(line);
			if (!curMessage.isPeriodic() && !curMessage.isDiagnosic()) {
				curBuf = curMessage.buf;
			} else if (curMessage.isPeriodic()) {
				curDelayIndex = curMessage.delay_index;
				curDelayCount = curMessage.delay_count;
			}
			for (Iterator<MessageEntry> iter = newMessages.listIterator(); iter.hasNext();) {
				MessageEntry curNewMessage = iter.next();
				if (curMessage.id == curNewMessage.id) {
					curMessage.source |= curNewMessage.source;
					curMessage.dest |= curNewMessage.dest;
					finalNewMessagesCount--;
					iter.remove();
					break;
				}
			}
			sb.append(MessageEntry.encode(curMessage));
		}

		// write the result to file
		BufferedWriter writer = new BufferedWriter(new FileWriter("result.txt"));
		writer.write(sb.toString());
		writer.close();


		// dealing with config file
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));

		int send_queue_max_num0 = curDelayIndex + curDelayIndex + 16;
		int table_id_max_num = 0 + finalNewMessagesCount;
	}

	private boolean isEntry(String line) {
		// @Robustness: this way of determining whether a line is an entry is naive...
		if (line.contains("0x")) {
			return true;
		} else {
			return false;
		}
	}

	private ArrayList<MessageEntry> decodeMessages() throws IOException {
		ArrayList<MessageEntry> newMessages = new ArrayList<>();
		FileReader fr = new FileReader(newMessagesFile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null) {
			if ("".equals(line.trim())) continue;
			newMessages.add(MessageEntry.decode(line));
		}
		return newMessages;
	}

	private short nextBuf(short buf) {
		if (buf >= 1 && buf <= 9) {
			return (short)(buf + 1);
		} else if (buf == 10) {
			return 1;
		} else {
			throw new RuntimeException("error: can not deal with this buf number: " + buf);
		}
	}

}
