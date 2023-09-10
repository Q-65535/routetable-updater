package parser;

import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.util.regex.*;

public class TableProcessor {
	File table;
	File config;
	File newMessagesFile;

	public TableProcessor(String tablePath, String configPath, String newMessagesPath) {
		table = new File(tablePath);
		config = new File(configPath);
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
		br.close();

		// write the result to file
		BufferedWriter writer = new BufferedWriter(new FileWriter("table_result.h"));
		writer.write(sb.toString());
		writer.close();

		changeConfig(curDelayIndex + curDelayCount, finalNewMessagesCount);
	}

	// dealing with config file
	private void changeConfig(int nextDelayIndex, int newMessageCount) throws IOException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
		LocalDateTime now = LocalDateTime.now();
		String date = dtf.format(now);

		StringBuilder sb = new StringBuilder();
		int send_queue_max_num0; 
		int table_id_max_num; 
		FileReader fr = new FileReader(config);
		BufferedReader br = new BufferedReader(fr);

		String line;
		while ((line = br.readLine()) != null) {
			// match date pattern
			Pattern datePattern = Pattern.compile("(2\\d{3}\\.(0[1-9]|1[0-2])\\.(0[1-9]|[12]\\d|3[01]))");
			Matcher matcher = datePattern.matcher(line);


			if (matcher.find()) {
				System.out.println("found date matching string");
				String found = matcher.group();
				line = line.replace(found, date);
			// 内部发送缓存深度
			} else if (line.contains("CAN_GATEWAY_SEND_QUEUE_MAX_NUM0")) {
				System.out.println("found can gateway send queue variable");
				Pattern hexPattern = Pattern.compile("0x..");
				matcher = hexPattern.matcher(line);
				if (matcher.find()) {
					String found = matcher.group();
					int can_gateway_max_num = Integer.decode(found);
					// if (can_gateway_max_num - nextDelayIndex <= 8) {
						can_gateway_max_num = nextDelayIndex + 16;
					// }
					String can_gateway_max_num_str = "0x" + String.format("%1$02X",can_gateway_max_num);
					line = line.replace(found, can_gateway_max_num_str);
				}
			// 路由表最大数量
			} else if (line.contains("CAN_ROUTE_TABLE_ID_MAX_NUM")) {
				System.out.println("found can routable number variable");
				Pattern hexPattern = Pattern.compile("0x..");
				matcher = hexPattern.matcher(line);
				if (matcher.find()) {
					String found = matcher.group();
					int can_table_max_num = Integer.decode(found);
					can_table_max_num += newMessageCount;
					String can_table_max_num_str = "0x" + String.format("%1$02X",can_table_max_num);
					line = line.replace(found, can_table_max_num_str);
				}
			} else if (line.contains("GATEWAY_ROUTE_TABLE_VERSION")) {
				Pattern versionPattern = Pattern.compile("\\{.*}");
				matcher = versionPattern.matcher(line);
				if (matcher.find()) {
					String found = matcher.group();
					TableVersionEntry tve = TableVersionEntry.decode(found);
					tve.update();
					int startIndex = matcher.start();
					int endIndex = matcher.end();
					line = line.substring(0, startIndex) + tve.encode() + line.substring(endIndex, line.length());
				}
			}
			sb.append(line + "\n");
		}
		br.close();

		// write the result to file
		BufferedWriter writer = new BufferedWriter(new FileWriter("config_result.h"));
		writer.write(sb.toString());
		writer.close();
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
