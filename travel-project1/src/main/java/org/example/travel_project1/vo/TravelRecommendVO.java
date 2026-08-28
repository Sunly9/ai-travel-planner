package org.example.travel_project1.vo;
import lombok.Data;
import java.util.List;

@Data
public class TravelRecommendVO {
    private String city;
    private Boolean success;
    private Integer days;
    private Double totalBudget;
    private List<DailyItinerary> dailyItinerary;
    private BudgetBreakdown budgetBreakdown;
    private List<String> tips;
    private List<String> warnings;
    private String error;
    private String rawResponse;

    @Data
    public static class DailyItinerary {
        private Integer day;
        private String date;
        private Timeslot morning;
        private Timeslot afternoon;
        private Timeslot evening;
    }

    @Data
    public static class Timeslot{
        private String spot;
        private String duration;
        private String transportation;
        private String description;
        private String ticket;
    }

    @Data
    public static class BudgetBreakdown{
        private Double accommodation;
        private Double food;
        private Double transportation;
        private Double other;
        private Double tickets;
    }

}
