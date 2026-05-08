package fansirsqi.xposed.sesame.util;

/**
 * 时间格式化工具类
 * 提供各种时间差和剩余时间的人性化格式化功能
 */
public class TimeFormatter {
    
    /**
     * 时间常量
     */
    public static final long ONE_SECOND_MS = 1000L;
    public static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    public static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    public static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    
    /**
     * 格式化时间差为人性化的字符串
     * @param milliseconds 时差毫秒
     * @param showSign 是否显示正负号
     * @return 格式化后的时间字符串
     */
    public static String formatTimeDifference(long milliseconds, boolean showSign) {
        long absMillis = Math.abs(milliseconds);
        String sign = showSign && milliseconds >= 0 ? "+" : showSign ? "-" : "";
        
        if (absMillis < ONE_MINUTE_MS) {
            return sign + (absMillis / ONE_SECOND_MS) + "秒";
        } else if (absMillis < ONE_HOUR_MS) {
            return sign + (absMillis / ONE_MINUTE_MS) + "分钟";
        } else if (absMillis < ONE_DAY_MS) {
            return sign + (absMillis / ONE_HOUR_MS) + "小时";
        } else {
            return sign + (absMillis / ONE_DAY_MS) + "天";
        }
    }
    
    /**
     * 格式化时间差为人性化的字符串（带正负号）
     */
    public static String formatTimeDifference(long milliseconds) {
        return formatTimeDifference(milliseconds, true);
    }
    
    /**
     * 格式化剩余时间（不带正负号）
     */
    public static String formatRemainingTime(long milliseconds) {
        return formatDetailedRemainingTime(Math.abs(milliseconds));
    }
    
    /**
     * 格式化详细的剩余时间（显示天、小时、分钟）
     * @param milliseconds 毫秒数
     * @return 详细的时间字符串，如 "1天2小时3分钟"
     */
    public static String formatDetailedRemainingTime(long milliseconds) {
        if (milliseconds < ONE_MINUTE_MS) {
            return (milliseconds / ONE_SECOND_MS) + "秒";
        } else if (milliseconds < ONE_HOUR_MS) {
            long minutes = milliseconds / ONE_MINUTE_MS;
            long seconds = (milliseconds % ONE_MINUTE_MS) / ONE_SECOND_MS;
            return seconds > 0 ? minutes + "分钟" + seconds + "秒" : minutes + "分钟";
        } else if (milliseconds < ONE_DAY_MS) {
            long hours = milliseconds / ONE_HOUR_MS;
            long minutes = (milliseconds % ONE_HOUR_MS) / ONE_MINUTE_MS;
            return minutes > 0 ? hours + "小时" + minutes + "分钟" : hours + "小时";
        } else {
            long days = milliseconds / ONE_DAY_MS;
            long hours = (milliseconds % ONE_DAY_MS) / ONE_HOUR_MS;
            long minutes = ((milliseconds % ONE_DAY_MS) % ONE_HOUR_MS) / ONE_MINUTE_MS;
            
            StringBuilder result = new StringBuilder().append(days).append("天");
            if (hours > 0) {
                result.append(hours).append("小时");
            }
            if (minutes > 0) {
                result.append(minutes).append("分钟");
            }
            return result.toString();
        }
    }
}
