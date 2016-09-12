package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataSources {
    public static Map<Integer, String> TABLE_NAMES = ImmutableMap.<Integer, String>builder()
            .put(1, "iowa_liquor").put(2, "nyc_taxi").put(3, "boston_taxi_pickups")
            .put(4, "fridge_data").put(5, "campaign_expenditures").put(6, "vmware_data")
            .put(7, "google_network").put(8, "art_daily_jumpsdown").put(9, "TravelTime_387")
            .put(10, "ethylene_CO").put(11, "daily-minimum-temperatures-in-me")
            .put(12, "number-of-daily-births-in-quebec")
            .put(13, "internet-traffic-data-in-bits-fr")
            .put(14, "monthly-temperature-in-england-f")
            .put(15, "Dodgers")
            .put(16, "household_power_consumption")
            .put(17, "online_retail_price")
            .put(18, "building_count")
            .build();
    public static Map<Integer, Long> WINDOW_RANGES = ImmutableMap.<Integer, Long>builder()
            .put(1, 180 * 24 * 3600 * 1000L).put(2, 20 * 24 * 3600 * 1000L)
            .put(3, 180 * 24 * 3600 * 1000L).put(4, 60 * 24 * 3600 * 1000L)
            .put(5, 180 * 24 * 3600 * 1000L).put(6, 80 * 24 * 3600 * 1000L)
            .put(7, 7 * 24 * 3600 * 1000L).put(8, 14 * 24 * 3600 * 1000L)
            .put(9, 60 * 24 * 3600 * 1000L)
            .put(10, 42087550L)
            .put(11, 9 * 365 * 24 * 3600 * 1000L)
            .put(12, 441504000000L) // 14 years
            .put(13, 5965200000L) // 1657 hours
            .put(14, 7820928000000L) // 248 years
            .put(15, 25 * 7 * 24 * 3600 * 1000L) // 25 weeks
            .put(16, 47 * 30 * 24 * 3600 * 1000L) // 47 months
            .put(17, 8 * 30 * 3600 * 1000L) // 8 months
            .put(18, 15 * 7 * 24 * 3600 * 1000L) // 15 weeks
            .build();
    public static Map<Integer, String> TIME_FORMATS = ImmutableMap.<Integer, String>builder()
            .put(10, "sec")
            .put(11, "MM/dd/yy")
            .put(12, "MM/dd/yy")
            .put(13, "MM/dd/yy hh:mm")
            .put(14, "yyyy-MM")
            .put(15, "MM/dd/yyyy hh:mm")
            .put(16, "dd/MM/yyyy hh:mm:ss")
            .put(17, "MM/dd/yyyy hh:mm")
            .put(18, "MM/dd/yyyy hh:mm:ss")
            .build();
    public static Map<Integer, List<String>> COLUMN_NAMES = ImmutableMap.<Integer, List<String>>builder()
            .put(1, new ArrayList<>(Arrays.asList(" date"," total")))
            .put(2, new ArrayList<>(Arrays.asList("pickup_datetime","passenger_count")))
            .put(3, new ArrayList<>(Arrays.asList("pickup_time", "NULL")))
            .put(4, new ArrayList<>(Arrays.asList("timestamp","reading")))
            .put(5, new ArrayList<>(Arrays.asList("contb_receipt_dt", "contb_receipt_amt")))
            .put(6, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(7, new ArrayList<>(Arrays.asList("date", "value")))
            .put(8, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(9, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(10, new ArrayList<>(Arrays.asList("Time (seconds)","s3")))
            .put(11, new ArrayList<>(Arrays.asList("Date",
                    "Daily minimum temperatures in Melbourne")))
            .put(12, new ArrayList<>(Arrays.asList("Date",
                    "Number of daily births in Quebec")))
            .put(13, new ArrayList<>(Arrays.asList("Time",
                    "Internet traffic data (in bits) from an ISP")))
            .put(14, new ArrayList<>(Arrays.asList("Month","Monthly temperature in England")))
            .put(15, new ArrayList<>(Arrays.asList("Time", "Count")))
            .put(16, new ArrayList<>(Arrays.asList("Time", "Global_active_power")))
            .put(17, new ArrayList<>(Arrays.asList("Time", "Price")))
            .put(18, new ArrayList<>(Arrays.asList("Time", "Count")))
            .build();
}
