package Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class Utilities {
    public static String getCurrentTime() {
        DateTime curTime = DateTime.now(DateTimeZone.forID("America/Los_Angeles"));
        org.joda.time.format.DateTimeFormatter fmt = DateTimeFormat.forPattern("dd MMM kk:mm:ss");
        return fmt.print(curTime);
    }
}