package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class DataSources {
    public static Map<Integer, String> TABLE_NAMES = ImmutableMap.<Integer, String>builder()
            .put(1, "iowa_liquor").put(2, "nyc_taxi")
            .put(8, "art_daily_jumpsdown").put(9, "TravelTime_387")
            .put(10, "ethylene_CO")
            .put(14, "monthly-temperature-in-england-f") // Here!
            .put(15, "Dodgers")
            .put(16, "household_power_consumption")
            .put(17, "online_retail_price")
            .put(18, "building_count")
            .put(19, "Twitter_volume_AAPL")
            .put(21, "cpu_utilization_asg_misconfiguration")
            .put(22, "machine_temperature_system_failure")
            .put(23, "nyc_taxi_3_mon")
            .put(24, "trafficData158324")
            .put(27, "nyc_taxi_oct_nov") // HERE!
            .put(3, "boston_taxi_pickups")
            .put(4, "fridge_data")
            .put(5, "campaign_expenditures")
            .put(6, "vmware_data")
            .put(7, "google_network_48")
            .put(28, "google_network_sim")
            .put(29, "Beijing_2016_HourlyPM25_created20161101")
            .put(30, "hilary_contrib_daily")
            .put(31, "NYSE_daily")
            .put(32, "NYSE_historical")
            .put(33, "traffic_violations")
            .put(34, "traffic_violations_2")
            .put(35, "TEK16_subset")
            .put(36, "qtdbsel102")
            .put(37, "nprs43")
            .put(38, "simulated")
            .put(39, "nprs44")
            .put(40, "fig11")
            .put(41, "fig12")
            .put(42, "fig15")
            .put(43, "uber")
            .put(44, "uber-minute")
            .put(45, "uber-may")
            .put(46, "fig10")
            .put(47, "eeg")
            .put(48, "ltstdb_20221_43")
            .put(0, "nyc_taxi")
            .build();
    public static Map<Integer, Long> WINDOW_RANGES = ImmutableMap.<Integer, Long>builder()
            .put(0, 150 * 24 * 3600 * 1000L)
            .put(1, 180 * 24 * 3600 * 1000L).put(2, 20 * 24 * 3600 * 1000L)
            .put(3, 180 * 24 * 3600 * 1000L).put(4, 60 * 24 * 3600 * 1000L)
            .put(5, 180 * 24 * 3600 * 1000L).put(6, 80 * 24 * 3600 * 1000L)
            .put(7, 7 * 24 * 3600 * 1000L).put(8, 14 * 24 * 3600 * 1000L)
            .put(9, 60 * 24 * 3600 * 1000L)
            //.put(10, 42087550L)
            .put(10, 36000000L)
            .put(14, 7820928000000L) // 248 years
            //.put(14, 7568640000000L) // 240 years
            .put(15, 30 * 24 * 3600 * 1000L) // 25 weeks
            //.put(15, 4 * 7 * 24 * 3600 * 1000L) // 15 weeks
            .put(16, 47 * 30 * 24 * 3600 * 1000L) // 47 months
            .put(17, 8 * 30 * 3600 * 1000L) // 8 months
            .put(18, 15 * 7 * 24 * 3600 * 1000L) // 15 weeks
            .put(19, 50 * 24 * 3600 * 1000L) // 50 days
            .put(20, 7 * 30 * 24 * 3600 * 1000L) // 7 months
            .put(21, 14 * 24 * 3600 * 1000L) // 50 days
            .put(22, 70 * 24 * 3600 * 1000L) // 70 days
            //.put(22, 63 * 24 * 3600 * 1000L) // 70 days
            .put(23, 90 * 24 * 3600 * 1000L) // 90 days
            .put(24, 100 * 24 * 3600 * 1000L) // 100 days
            .put(27, 75 * 24 * 3600 * 1000L) // 75 days
            .put(28, 7 * 24 * 3600 * 1000L)
            .put(29, 6576 * 3600 * 1000L) // 9 months (window size = 1)
            .put(30, 510 * 24 * 3600 * 1000L)
            .put(31, 1700 * 24 * 3600 * 1000L)
            .put(32, 1500 * 24 * 3600 * 1000L)
            .put(33, 1000 * 24 * 3600 * 1000L)
            .put(34, 400 * 24 * 3600 * 1000L)
            .put(35, 3000 * 1000L)
            .put(36, 180 * 1000L)
            .put(37, 18000 * 1000L)
            .put(38, 800 * 1000L)
            .put(39, 20000 * 1000L)
            .put(40, 60 * 1000L)
            .put(41, 60 * 1000L)
            .put(42, 35040 * 1000L)
            .put(43, 182 * 24 * 3600 * 1000L)
            .put(44, 150 * 24 * 3600 * 1000L)
            .put(45, 31 * 24 * 3600 * 1000L)
            .put(46, 7200 * 1000L)
            .put(47, 15 * 1000L)
            .put(48, 15 * 1000L)
            .build();
    public static Map<Integer, Long> INTERVALS = ImmutableMap.<Integer, Long>builder()
            .put(8, 5 * 60 * 1000L)
            .put(10, 10L)
            .put(14, 30 * 24 * 60 * 60 * 1000L)
            .put(15, 5 * 60 * 1000L)
            .put(19, 5 * 60 * 1000L)
            .put(21, 5 * 60 * 1000L)
            .put(22, 5 * 60 * 1000L)
            .put(24, 5 * 60 * 1000L)
            .put(27, 30 * 60 * 1000L)
            .put(0, 30 * 60 * 1000L)
            .put(28, 5 * 60 * 1000L)
            .put(29, 60 * 60 * 1000L)
            .put(30, 24 * 60 * 60 * 1000L)
            .put(31, 24 * 60 * 60 * 1000L)
            .put(32, 24 * 60 * 60 * 1000L)
            .put(33, 24 * 60 * 60 * 1000L)
            .put(34, 24 * 60 * 60 * 1000L)
            .put(35, 1000L)
            .put(36, 1L)
            .put(37, 1000L)
            .put(38, 1000L)
            .put(39, 1000L)
            .put(40, 1L)
            .put(41, 1L)
            .put(42, 1000L)
            .put(43, 60 * 60 * 1000L)
            .put(44, 60 * 1000L)
            .put(45, 60 * 1000L)
            .put(46, 1000L)
            .put(47, 1L)
            .put(48, 1L)
            .build();
    public static Map<Integer, String> TIME_FORMATS = ImmutableMap.<Integer, String>builder()
            .put(5, "yyyy-MM-dd")
            .put(6, "MM/dd/yy hh:mm")
            .put(8, "yyyy-MM-dd HH:mm")
            .put(10, "sec")
            .put(14, "yyyy-MM")
            .put(15, "MM/dd/yyyy HH:mm")
            .put(16, "dd/MM/yyyy HH:mm:ss")
            .put(17, "MM/dd/yyyy HH:mm")
            .put(18, "MM/dd/yyyy HH:mm:ss")
            .put(19, "yyyy-MM-dd HH:mm:ss")
            .put(20, "yyyy-MM-dd HH:mm:ss")
            .put(21, "yyyy-MM-dd HH:mm:ss")
            //.put(21, "MM/dd/yy HH:mm")
            .put(22, "yyyy-MM-dd HH:mm:ss")
            .put(23, "MM/dd/yy hh:mm")
            .put(24, "yyyy-MM-dd'T'HH:mm:ss")
            .put(27, "MM/dd/yy HH:mm")
            .put(0, "MM/dd/yy HH:mm")
            .put(28, "yyyy-MM-dd hh:mm:ss")
            .put(29, "MM/dd/yy hh:mm")
            .put(30, "MM/dd/yy")
            .put(31, "MM/dd/yy")
            .put(32, "MM/dd/yy")
            .put(33, "yyyy-MM-dd")
            .put(34, "MM/dd/yy")
            .put(35, "sec")
            .put(36, "sec")
            .put(37, "sec")
            .put(38, "sec")
            .put(39, "sec")
            .put(40, "sec")
            .put(41, "sec")
            .put(42, "sec")
            .put(43, "MM/dd/yy hh:mm")
            .put(44, "MM/dd/yy hh:mm")
            .put(45, "MM/dd/yy hh:mm")
            .put(46, "sec")
            .put(47, "sec")
            .put(48, "sec")
            .build();
    public static Map<Integer, List<String>> COLUMN_NAMES = ImmutableMap.<Integer, List<String>>builder()
            .put(1, new ArrayList<>(Arrays.asList(" date"," total")))
            .put(2, new ArrayList<>(Arrays.asList("pickup_datetime","passenger_count")))
            .put(3, new ArrayList<>(Arrays.asList("pickup_time", "NULL")))
            .put(4, new ArrayList<>(Arrays.asList("timestamp","reading")))
            .put(5, new ArrayList<>(Arrays.asList("contb_receipt_dt", "contb_receipt_amt")))
            .put(6, new ArrayList<>(Arrays.asList("time","value")))
            .put(7, new ArrayList<>(Arrays.asList("date", "value")))
            .put(8, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(9, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(10, new ArrayList<>(Arrays.asList("Time (seconds)","s3")))
            .put(14, new ArrayList<>(Arrays.asList("Month","Monthly temperature in England")))
            .put(15, new ArrayList<>(Arrays.asList("Time", "Count")))
            .put(16, new ArrayList<>(Arrays.asList("Time", "Global_active_power")))
            .put(17, new ArrayList<>(Arrays.asList("Time", "Price")))
            .put(18, new ArrayList<>(Arrays.asList("Time", "Count")))
            .put(19, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(20, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(21, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(22, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(23, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(24, new ArrayList<>(Arrays.asList("TIMESTAMP", "vehicleCount")))
            .put(27, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(0, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(28, new ArrayList<>(Arrays.asList("timestamp", "value")))
            .put(29, new ArrayList<>(Arrays.asList("Date (LST)", "Value")))
            .put(30, new ArrayList<>(Arrays.asList("disb_dt", "amt")))
            .put(31, new ArrayList<>(Arrays.asList("Trade Date", "NYSE Group Dollar Volume")))
            .put(32, new ArrayList<>(Arrays.asList("Trade Date", "NYSE Group Dollar Volume")))
            .put(33, new ArrayList<>(Arrays.asList("date", "violations")))
            .put(34, new ArrayList<>(Arrays.asList("vio_date", "violations")))
            .put(35, new ArrayList<>(Arrays.asList("time", "value")))
            .put(36, new ArrayList<>(Arrays.asList("time", "val2")))
            .put(37, new ArrayList<>(Arrays.asList("time", "value")))
            .put(38, new ArrayList<>(Arrays.asList("id", "value")))
            .put(39, new ArrayList<>(Arrays.asList("time", "value")))
            .put(40, new ArrayList<>(Arrays.asList("time", "val1")))
            .put(41, new ArrayList<>(Arrays.asList("col1", "col3")))
            .put(42, new ArrayList<>(Arrays.asList("time", "value")))
            .put(43, new ArrayList<>(Arrays.asList("time", "value")))
            .put(44, new ArrayList<>(Arrays.asList("time", "value")))
            .put(45, new ArrayList<>(Arrays.asList("time", "value")))
            .put(46, new ArrayList<>(Arrays.asList("time", "value")))
            .put(47, new ArrayList<>(Arrays.asList("time", "val1")))
            .put(48, new ArrayList<>(Arrays.asList("time", "val2")))
            .build();
}
