package blockchain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeChecker {
	private static String DATE_FORMAT = "HH:mm:ss.SSS, yyyy-MM-dd";
	private static String TIME_ZONE = "Asia/Seoul";

	public static long ONE_MINUTE = 1000 * 60;
	public static long ONE_HOUR = ONE_MINUTE * 60;
	public static long FIFTY_NINE_MINS = ONE_HOUR - ONE_MINUTE;
	public static long INDEX_TIME_VALUE = 500000;
	static SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.KOREA);
	static { formatter.setTimeZone(TimeZone.getTimeZone(TIME_ZONE)); }
	
	public static String getCurrentTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone(TIME_ZONE)); 
		return formatter.format(calendar.getTimeInMillis());
	}
	
	public static String getIndexTime() {
		String currentTime = getCurrentTime();
		
		String res = "-1";
		
		try {
			long numIndexDate = (long) (Math.floor(formatter.parse(currentTime).getTime() / INDEX_TIME_VALUE) * INDEX_TIME_VALUE) - INDEX_TIME_VALUE;
			Date date = new Date(numIndexDate);
			res = formatter.format(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
		return res;
	}
	
	public static long getTimeLong(String time) {
		Date timeDate;
		try {
			timeDate = formatter.parse(time);
			return timeDate.getTime();
		} catch (ParseException e) {
			return -1;
		}
	}
	
	public static String getPreviousIndexTime() {
		String currentTime = getCurrentTime();
		
		String res = "-1";
		
		try {
			long numIndexDate = (long) ((Math.floor(formatter.parse(currentTime).getTime() - INDEX_TIME_VALUE) / INDEX_TIME_VALUE) * INDEX_TIME_VALUE) - INDEX_TIME_VALUE;
			Date date = new Date(numIndexDate);
			res = formatter.format(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
	
	public static int getIndexPhase() {
		int trial = 0;
		while(true) {
			try {
				if(((long)(formatter.parse(getCurrentTime()).getTime() / INDEX_TIME_VALUE) % 2) == 0){
					return 0;
				} else {
					return 1;
				}
			} catch (ParseException e) {
				if(trial > 18) {
					e.printStackTrace();
					return -1;
				}
				continue;
			}
		}
	}
	
	// if time1 is slower than time2, it returns true;
	public static boolean compare(String time1, String time2) {
		boolean res = false;
		try {
			Date date1 = formatter.parse(time1);
			Date date2 = formatter.parse(time2);
			
			long gap = date1.getTime() - date2.getTime();
			if(gap > 0) {
				res = true; 
			} else {
				res = false;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	public static long getTimeGap(String time1, String time2) {
		
		try {
			Date date1 = formatter.parse(time1);
			Date date2 = formatter.parse(time2);
			
			long gap = date1.getTime() - date2.getTime();
			
			return gap;
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static boolean checkBlockTime(String birth, String time) {
		long indexGapNode = TimeChecker.getTimeGap(birth, time);
		if(indexGapNode >= 0) return false;
		else return true;
	}
}
