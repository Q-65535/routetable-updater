import parser.*;

import java.io.IOException;
import java.time.*;
import java.time.format.*;

public class Main {
    public static void main(String[] args) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));

		System.out.println("Working Directory = " + System.getProperty("user.dir"));
        EntryParser np = new EntryParser();
        String entry = "{0x00000437,    0x08,   0x08,    0x10, {0x00,0x00,0x00,0x00,0x00},    {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0}, 0x00,   0x00},";
        MessageEntry me = MessageEntry.decode(entry);
		MessageEntry.encode(me);

        try {
            TableProcessor tp = new TableProcessor("table.h", "new_messages.txt");
            tp.process();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
